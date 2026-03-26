package org.example.bazon.services;

import jakarta.annotation.PostConstruct;

import org.example.bazon.models.*;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MockStorageService {

    // Имитация таблиц БД
    private final Map<String, Picker> pickers = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    // Имитация сессии (токен -> ID сборщика)
    private final Map<String, String> activeTokens = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 1. Создаем сборщиков
        Picker manager = new Picker("manager", "admin123", "MANAGER");
        pickers.put(manager.getId(), manager);

        // Создаем сборщиков с ролью
        Picker picker1 = new Picker("ivan_collector", "123", "PICKER");
        Picker picker2 = new Picker("maria_collector", "123", "PICKER");
        pickers.put(picker1.getId(), picker1);
        pickers.put(picker2.getId(), picker2);

        // 2. Создаем Заказ №1 (Электроника)
        Order order1 = new Order();
        order1.getItems().add(new OrderItem("Смартфон Apple iPhone 15 Pro 256GB", 2));
        order1.getItems().add(new OrderItem("Чехол силиконовый черный", 2));
        order1.getItems().add(new OrderItem("Защитное стекло", 2));
        orders.put(order1.getId(), order1);

        // 3. Создаем Заказ №2 (Бытовая техника)
        Order order2 = new Order();
        order2.getItems().add(new OrderItem("Робот-пылесос Xiaomi S10", 1));
        order2.getItems().add(new OrderItem("Фильтр для пылесоса (набор)", 1));
        orders.put(order2.getId(), order2);

        // 4. Создаем Заказ №3 (Крупногабарит)
        Order order3 = new Order();
        order3.getItems().add(new OrderItem("Монитор Samsung Odyssey 32\"", 2));
        order3.getItems().add(new OrderItem("Кронштейн для монитора", 2));
        order3.getItems().add(new OrderItem("HDMI кабель 2м", 2));
        orders.put(order3.getId(), order3);

        System.out.println(">>> MOCK DATA INITIALIZED. ORDERS: " + orders.size());
    }

    // --- Auth Methods ---
    // --- Admin Methods (для менеджера) ---

    public Picker createPicker(String login, String password, String role) {
        // Проверка на дубликат логина
        if (pickers.values().stream().anyMatch(p -> p.getLogin().equals(login))) {
            return null;
        }
        Picker newPicker = new Picker(login, password, role);
        pickers.put(newPicker.getId(), newPicker);
        return newPicker;
    }

    public List<Picker> getAllPickers() {
        return new ArrayList<>(pickers.values());
    }
    public boolean hasActiveOrder(String pickerId) {
        return orders.values().stream()
                .anyMatch(o -> pickerId.equals(o.getAssignedPickerId())
                        && o.getStatus() != OrderStatus.COMPLETED);
    }

    // Получить активный заказ сборщика (если есть)
    public Order getActiveOrder(String pickerId) {
        return orders.values().stream()
                .filter(o -> pickerId.equals(o.getAssignedPickerId())
                        && o.getStatus() != OrderStatus.COMPLETED)
                .findFirst()
                .orElse(null);
    }

    public boolean deactivatePicker(String pickerId) {
        Picker picker = pickers.get(pickerId);
        if (picker != null) {
            // Просто меняем пароль на невалидный (простой способ "деактивации" без новых полей)
            picker.setPassword("DEACTIVATED_" + UUID.randomUUID());
            return true;
        }
        return false;
    }

    // Обнови login() для проверки активности (опционально):
    public String login(String login, String password) {
        Picker picker = pickers.values().stream()
                .filter(p -> p.getLogin().equals(login) && p.getPassword().equals(password))
                .findFirst()
                .orElse(null);

        if (picker != null) {
            // Не пускаем деактивированных
            if (picker.getPassword().startsWith("DEACTIVATED_")) {
                return null;
            }
            String token = UUID.randomUUID().toString();
            activeTokens.put(token, picker.getId());
            return token;
        }
        return null;
    }


    public Picker getPickerByToken(String token) {
        String pickerId = activeTokens.get(token);
        return pickerId != null ? pickers.get(pickerId) : null;
    }

    // --- Order Methods ---
    public List<Order> getAvailableOrders() {
        return orders.values().stream()
                .filter(o -> o.getStatus() == OrderStatus.NEW)
                .collect(Collectors.toList());
    }

    public List<Order> getMyOrders(String pickerId) {
        return orders.values().stream()
                .filter(o -> pickerId.equals(o.getAssignedPickerId()) && o.getStatus() != OrderStatus.COMPLETED)
                .collect(Collectors.toList());
    }

    public boolean claimOrder(String orderId, String pickerId) {
        Order order = orders.get(orderId);

        // Проверка 1: заказ существует и новый
        if (order == null || order.getStatus() != OrderStatus.NEW) {
            return false;
        }

        // Проверка 2: у сборщика нет другого активного заказа
        if (hasActiveOrder(pickerId)) {
            return false; // <-- Блокируем, если уже есть заказ в работе
        }

        // Всё ок — закрепляем
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setAssignedPickerId(pickerId);
        return true;
    }

    public boolean updateItemStatus(String orderId, String itemId, ItemStatus newStatus) {
        Order order = orders.get(orderId);
        if (order == null) return false;

        for (OrderItem item : order.getItems()) {
            if (item.getId().equals(itemId)) {
                item.setStatus(newStatus);
                checkOrderCompletion(order);
                return true;
            }
        }
        return false;
    }

    private void checkOrderCompletion(Order order) {
        boolean allDone = order.getItems().stream()
                .allMatch(i -> i.getStatus() == ItemStatus.PICKED || i.getStatus() == ItemStatus.NOT_FOUND);

        if (allDone) {
            order.setStatus(OrderStatus.COMPLETED);
        }
    }
}
