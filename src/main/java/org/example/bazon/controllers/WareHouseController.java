package org.example.bazon.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.bazon.models.ItemStatus;
import org.example.bazon.models.Order;
import org.example.bazon.models.Picker;
import org.example.bazon.services.MockStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Склад Bazon", description = "API для мобильного приложения сборщиков")
public class WareHouseController {

    private final MockStorageService storage;

    @Operation(summary = "Авторизация сборщика", description = "Возвращает токен доступа для последующих запросов")
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String token = storage.login(credentials.get("login"), credentials.get("password"));
        if (token != null) {
            return ResponseEntity.ok(Map.of("token", token, "message", "Успешный вход"));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Неверный логин или пароль"));
    }

    @Operation(summary = "Получить доступные заказы", description = "Список заказов со статусом NEW, которые никто не взял")
    @GetMapping("/orders/available")
    public ResponseEntity<List<Order>> getAvailableOrders(
            @Parameter(description = "Токен авторизации", required = true)
            @RequestHeader("X-Auth-Token") String token) {

        if (storage.getPickerByToken(token) == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(storage.getAvailableOrders());
    }

    @Operation(summary = "Закрепить заказ за собой", description = "Переводит заказ в статус IN_PROGRESS")
    @PostMapping("/orders/{orderId}/claim")
    public ResponseEntity<?> claimOrder(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable String orderId) {

        Picker picker = storage.getPickerByToken(token);
        if (picker == null) return ResponseEntity.status(401).build();

        // Проверка: есть ли уже активный заказ
        if (storage.hasActiveOrder(picker.getId())) {
            Order active = storage.getActiveOrder(picker.getId());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "У вас уже есть активный заказ",
                    "activeOrderId", active != null ? active.getId() : null
            ));
        }

        boolean success = storage.claimOrder(orderId, picker.getId());
        return success ? ResponseEntity.ok(Map.of("message", "Заказ закреплен"))
                : ResponseEntity.badRequest().body(Map.of("error", "Заказ недоступен"));
    }

    @Operation(summary = "Мои активные заказы", description = "Заказы, которые я взял в работу")
    @GetMapping("/orders/my")
    public ResponseEntity<List<Order>> getMyOrders(@RequestHeader("X-Auth-Token") String token) {
        Picker picker = storage.getPickerByToken(token);
        if (picker == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(storage.getMyOrders(picker.getId()));
    }

    @Operation(summary = "Обновить статус товара", description = "Отметить товар как 'Собран' или 'Не найден'")
    @PutMapping("/orders/{orderId}/items/{itemId}/status")
    public ResponseEntity<?> updateItemStatus(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable String orderId,
            @PathVariable String itemId,
            @RequestParam ItemStatus status) {

        Picker picker = storage.getPickerByToken(token);
        if (picker == null) return ResponseEntity.status(401).build();

        // Проверка прав (принадлежит ли заказ сборщику)
        Order order = storage.getMyOrders(picker.getId()).stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst()
                .orElse(null);

        if (order == null) return ResponseEntity.status(403).body(Map.of("error", "Заказ не ваш или завершен"));

        boolean success = storage.updateItemStatus(orderId, itemId, status);
        return success ? ResponseEntity.ok(Map.of("message", "Статус обновлен"))
                : ResponseEntity.badRequest().build();
    }
}
