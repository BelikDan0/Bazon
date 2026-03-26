package org.example.bazon.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.bazon.models.Picker;
import org.example.bazon.services.MockStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Админка менеджера", description = "Регистрация и управление сборщиками")
public class ManagerController {

    private final MockStorageService storage;

    @Operation(summary = "Зарегистрировать сборщика", description = "Только для менеджеров (роль MANAGER)")
    @PostMapping("/pickers")
    public ResponseEntity<?> createPicker(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody Map<String, String> request) {

        Picker manager = storage.getPickerByToken(token);
        // Проверка: есть ли пользователь и является ли он менеджером
        if (manager == null || !"MANAGER".equals(manager.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещен"));
        }

        String login = request.get("login");
        String password = request.get("password");

        Picker newPicker = storage.createPicker(login, password, "PICKER");
        if (newPicker == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Логин уже занят"));
        }

        return ResponseEntity.status(201).body(Map.of(
                "id", newPicker.getId(),
                "login", newPicker.getLogin(),
                "message", "Сборщик создан"
        ));
    }

    @Operation(summary = "Список всех сборщиков", description = "Только для менеджеров")
    @GetMapping("/pickers")
    public ResponseEntity<?> getAllPickers(@RequestHeader("X-Auth-Token") String token) {
        Picker manager = storage.getPickerByToken(token);
        if (manager == null || !"MANAGER".equals(manager.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещен"));
        }

        // Возвращаем только безопасные данные (без паролей)
        List<Map<String, String>> list = storage.getAllPickers().stream()
                .map(p -> Map.of(
                        "id", p.getId(),
                        "login", p.getLogin(),
                        "role", p.getRole() != null ? p.getRole() : "PICKER"
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Деактивировать сборщика", description = "Только для менеджеров")
    @PostMapping("/pickers/{pickerId}/deactivate")
    public ResponseEntity<?> deactivatePicker(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable String pickerId) {

        Picker manager = storage.getPickerByToken(token);
        if (manager == null || !"MANAGER".equals(manager.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещен"));
        }

        boolean success = storage.deactivatePicker(pickerId);
        return success ? ResponseEntity.ok(Map.of("message", "Сборщик деактивирован"))
                : ResponseEntity.badRequest().body(Map.of("error", "Не найден"));
    }
}
