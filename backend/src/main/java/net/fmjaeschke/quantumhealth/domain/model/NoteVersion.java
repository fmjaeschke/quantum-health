package net.fmjaeschke.quantumhealth.domain.model;

import java.time.Instant;

public record NoteVersion(int version, String content, UserId authorId, Instant createdAt) {}
