package com.keianluxandor.backend.contact;

// For the admin dashboard badge, e.g. "Messages (3 new)"
public record ContactStats(long newMessages, long totalMessages) {
}