package com.aevum.bounties;

import java.util.UUID;

public record Bounty(
        UUID id,
        UUID target,
        String targetName,
        UUID creator,
        String creatorName,
        double amount,
        long createdAt
) {}
