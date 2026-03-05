package dev.duckslock.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import dev.duckslock.enclave.Enclave;

import java.util.Arrays;
import java.util.Locale;

public class DebugRoundCommand extends CommandBase {

    private final ArenaDebugRoundService debugService;

    public DebugRoundCommand(ArenaDebugRoundService debugService) {
        super("tddebuground", "Tower defense debug round controls.");
        this.debugService = debugService;
        setAllowsExtraArguments(true);
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        String[] tokens = normalizeTokens(ctx.getInputString());
        if (tokens.length == 0) {
            sendUsage(ctx);
            return;
        }

        String action = tokens[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "status" -> sendStatus(ctx);
            case "enable" -> {
                debugService.setEnabled(true);
                ctx.sendMessage(Message.raw("[TD] Debug round auto-spawn enabled."));
            }
            case "disable" -> {
                debugService.setEnabled(false);
                ctx.sendMessage(Message.raw("[TD] Debug round auto-spawn disabled."));
            }
            case "set" -> setRound(ctx, tokens);
            case "newonly" -> setNewOnly(ctx, tokens);
            case "spawn" -> spawnCommand(ctx, tokens);
            default -> sendUsage(ctx);
        }
    }

    private void setRound(CommandContext ctx, String[] tokens) {
        Integer roundId = parseInt(tokens, 1);
        if (roundId == null) {
            ctx.sendMessage(Message.raw("[TD] Usage: /tddebuground set <roundId>"));
            return;
        }
        if (DebugRoundDefinitions.get(roundId) == null) {
            ctx.sendMessage(Message.raw("[TD] Unknown round id: " + roundId
                    + " (available: " + DebugRoundDefinitions.ids() + ")"));
            return;
        }
        debugService.setActiveRoundId(roundId);
        ctx.sendMessage(Message.raw("[TD] Active debug round set to " + roundId + "."));
    }

    private void setNewOnly(CommandContext ctx, String[] tokens) {
        Integer toggle = parseInt(tokens, 1);
        if (toggle == null || (toggle != 0 && toggle != 1)) {
            ctx.sendMessage(Message.raw("[TD] Usage: /tddebuground newonly <1|0>"));
            return;
        }
        debugService.setTriggerOnlyOnNewAssignment(toggle == 1);
        ctx.sendMessage(Message.raw("[TD] Trigger mode set to new-assignment-only="
                + debugService.isTriggerOnlyOnNewAssignment()));
    }

    private void spawnCommand(CommandContext ctx, String[] tokens) {
        Integer enclaveIndex = parseInt(tokens, 1);
        Integer roundOverride = parseInt(tokens, 2);

        if (ctx.isPlayer()) {
            Enclave enclave = debugService.findEnclaveForPlayer(ctx.sender().getUuid());
            if (enclave == null) {
                ctx.sendMessage(Message.raw("[TD] You are not assigned to an enclave."));
                return;
            }

            if (enclaveIndex == null) {
                enclaveIndex = enclave.getIndex();
            } else if (roundOverride == null && DebugRoundDefinitions.get(enclaveIndex) != null) {
                // Player shortcut: /tddebuground spawn <roundId>
                roundOverride = enclaveIndex;
                enclaveIndex = enclave.getIndex();
            }
        } else if (enclaveIndex == null) {
            ctx.sendMessage(Message.raw("[TD] Console usage: /tddebuground spawn <enclaveIndex> [roundId]"));
            return;
        }

        int roundId = roundOverride != null ? roundOverride : debugService.getActiveRoundId();
        if (DebugRoundDefinitions.get(roundId) == null) {
            ctx.sendMessage(Message.raw("[TD] Unknown round id: " + roundId
                    + " (available: " + DebugRoundDefinitions.ids() + ")"));
            return;
        }

        boolean ok = debugService.spawnRoundForEnclave(enclaveIndex, roundId);
        if (!ok) {
            ctx.sendMessage(Message.raw("[TD] Failed to spawn round. Check enclave/round values."));
            return;
        }

        ctx.sendMessage(Message.raw("[TD] Spawned debug round " + roundId + " for enclave " + enclaveIndex + "."));
    }

    private void sendStatus(CommandContext ctx) {
        ctx.sendMessage(Message.raw("[TD] enabled=" + debugService.isEnabled()
                + ", activeRound=" + debugService.getActiveRoundId()
                + ", newOnly=" + debugService.isTriggerOnlyOnNewAssignment()
                + ", availableRounds=" + DebugRoundDefinitions.ids()));
    }

    private void sendUsage(CommandContext ctx) {
        ctx.sendMessage(Message.raw("[TD] Usage:"));
        ctx.sendMessage(Message.raw("[TD] /tddebuground status"));
        ctx.sendMessage(Message.raw("[TD] /tddebuground enable|disable"));
        ctx.sendMessage(Message.raw("[TD] /tddebuground set <roundId>"));
        ctx.sendMessage(Message.raw("[TD] /tddebuground newonly <1|0>"));
        ctx.sendMessage(Message.raw("[TD] /tddebuground spawn [enclaveIndex] [roundId]"));
    }

    private String[] normalizeTokens(String input) {
        if (input == null || input.isBlank()) {
            return new String[0];
        }

        String[] raw = input.trim().split("\\s+");
        if (raw.length == 0) {
            return raw;
        }

        String first = raw[0];
        if ("/tddebuground".equalsIgnoreCase(first) || "tddebuground".equalsIgnoreCase(first)) {
            return Arrays.copyOfRange(raw, 1, raw.length);
        }

        return raw;
    }

    private Integer parseInt(String[] tokens, int index) {
        if (index < 0 || index >= tokens.length) {
            return null;
        }

        try {
            return Integer.parseInt(tokens[index]);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
