package org.example.bazon.models;

import lombok.Data;

import java.util.UUID;

@Data
public class Picker {
    private String id;
    private String login;
    private String password;
    private String role;  // <-- ДОБАВИТЬ: "PICKER" или "MANAGER"

    public Picker(String login, String password, String role) {  // <-- Обновить конструктор
        this.id = UUID.randomUUID().toString();
        this.login = login;
        this.password = password;
        this.role = role;  // <-- Инициализировать роль
    }
}
