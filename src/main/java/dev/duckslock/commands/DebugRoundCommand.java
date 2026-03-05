package dev.duckslock.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import dev.duckslock.enclave.Enclave;

public class DebugRoundCommand extends CommandBase {

    private final ArenaDebugRoundService debugService;
    private final RequiredArg<String> actionArg;
    private final OptionalArg<Integer> arg1;
    private final OptionalArg<Integer> arg2;

    public DebugRoundCommand(ArenaDebugRoundService debugService) {
        super("tddebuground", "Tower defense debug round controls.");
        this.debugService = debugService;

        actionArg = withRequiredArg("action", "Action: status|enable|disable|set|spawn|newonly", ArgTypes.STRING);
        arg1 = withOptionalArg("arg1", "First integer argument", ArgTypes.INTEGER);
        arg2 = withOptionalArg("arg2", "Second integer argument", ArgTypes.INTEGER);
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        String action = ctx.get(actionArg).toLowerCase();
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
            case "set" -> {
                Integer roundId = getIntArg(ctx, arg1);
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
            case "newonly" -> {
                Integer toggle = getIntArg(ctx, arg1);
                if (toggle == null || (toggle != 0 && toggle != 1)) {
                    ctx.sendMessage(Message.raw("[TD] Usage: /tddebuground newonly <1|0>"));
                    return;
                }
                debugService.setTriggerOnlyOnNewAssignment(toggle == 1);
                ctx.sendMessage(Message.raw("[TD] Trigger mode set to new-assignment-only="
                        + debugService.isTriggerOnlyOnNewAssignment()));
            }
            case "spawn" -> spawnCommand(ctx);
            default -> sendUsage(ctx);
        }
    }

    private void spawnCommand(CommandContext ctx) {
        Integer enclaveIndex = getIntArg(ctx, arg1);
        Integer roundOverride = getIntArg(ctx, arg2);

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

    private Integer getIntArg(CommandContext ctx, OptionalArg<Integer> arg) {
        return ctx.provided(arg) ? ctx.get(arg) : null;
    }
}
