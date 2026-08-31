package com.itasocialacademy.oitassist.chat.realtime.handlers;

import com.itasocialacademy.oitassist.chat.event.ForumDomainEvent;

@FunctionalInterface
public interface ForumRealtimeProjectionHandler {
    void handle(ForumDomainEvent event);
}