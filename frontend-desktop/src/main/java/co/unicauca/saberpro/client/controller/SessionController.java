package co.unicauca.saberpro.client.controller;

import co.unicauca.saberpro.client.api.SaberProApiFacade;
import co.unicauca.saberpro.client.api.dto.NotificationDto;
import co.unicauca.saberpro.client.api.dto.UserDto;
import co.unicauca.saberpro.client.model.AppChange;
import co.unicauca.saberpro.client.model.AppEvents;

import java.awt.*;
import java.util.function.Consumer;

/** Controlador de sesion, registro de usuarios y bandeja de notificaciones. */
public class SessionController {

    private final SaberProApiFacade api;
    private final AppEvents events;
    private long lastUnread = -1;

    public SessionController(SaberProApiFacade api, AppEvents events) {
        this.api = api;
        this.events = events;
    }

    public void login(Component view, String login, String password, Consumer<UserDto> onSuccess) {
        UiTask.run(view, () -> api.login(login, password), onSuccess);
    }

    public void logout() {
        api.logout();
        lastUnread = -1;
    }

    public UserDto user() {
        return api.currentUser().orElseThrow();
    }

    public void register(Component view, String login, String name, String email, String role, String password,
                         Consumer<UserDto> onSuccess) {
        UiTask.run(view, () -> api.registerUser(login, name, email, role, password), onSuccess);
    }

    public void inbox(Component view, int page, Consumer<NotificationDto.Page> onSuccess) {
        UiTask.run(view, () -> api.inbox(page, 10), onSuccess);
    }

    public void markRead(Component view, long id) {
        UiTask.run(view, () -> { api.markNotificationRead(id); return null; },
                v -> events.publish(AppChange.NOTIFICATIONS));
    }

    /**
     * Sondeo silencioso del contador de no leidas. Cuando llega una
     * notificacion nueva (producida de forma asincrona por los eventos del
     * backend), se avisa a todas las vistas para que se refresquen.
     */
    public long pollUnread() {
        long unread = api.inbox(1, 1).unread();
        if (lastUnread >= 0 && unread > lastUnread) {
            events.publish(AppChange.NOTIFICATIONS);
            events.publish(AppChange.QUESTIONS);
            events.publish(AppChange.REVIEWS);
        }
        lastUnread = unread;
        return unread;
    }
}
