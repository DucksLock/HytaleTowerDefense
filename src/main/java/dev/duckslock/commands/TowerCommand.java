package dev.duckslock.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import dev.duckslock.enclave.Enclave;
import dev.duckslock.game.GameManager;
import dev.duckslock.tower.TowerManager;
import dev.duckslock.tower.TowerType;
import dev.duckslock.tower.UpgradePath;

import java.util.Arrays;
import java.util.Locale;

public class TowerCommand extends CommandBase {

    private final GameManager gameManager;

    public TowerCommand(GameManager gameManager) {
        super("tdtower", "Tower placement and management commands.");
        this.gameManager = gameManager;
        setAllowsExtraArguments(true);
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        String[] tokens = normalizeTokens(ctx.getInputString());
        if (tokens.length == 0) {
            sendUsage(ctx);
            return;
        }

        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("[TD] /tdtower is player-only for now."));
            return;
        }

        Enclave enclave = gameManager.findEnclaveForPlayer(ctx.sender().getUuid());
        if (enclave == null) {
            ctx.sendMessage(Message.raw("[TD] You are not assigned to an enclave."));
            return;
        }

        String action = tokens[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "place" -> handlePlace(ctx, enclave, tokens);
            case "sell" -> handleSell(ctx, enclave, tokens);
            case "upgrade" -> handleUpgrade(ctx, enclave, tokens);
            case "status" -> sendStatus(ctx, enclave);
            default -> sendUsage(ctx);
        }
    }

    private void handlePlace(CommandContext ctx, Enclave enclave, String[] tokens) {
        if (tokens.length < 4) {
            ctx.sendMessage(Message.raw("[TD] Usage: /tdtower place <type|wc3UnitId> <worldX> <worldZ>"));
            return;
        }

        TowerType type = TowerType.parse(tokens[1]);
        if (type == null) {
            ctx.sendMessage(Message.raw("[TD] Unknown tower type: " + tokens[1]));
            return;
        }

        Integer worldX = parseInt(tokens, 2);
        Integer worldZ = parseInt(tokens, 3);
        if (worldX == null || worldZ == null) {
            ctx.sendMessage(Message.raw("[TD] worldX/worldZ must be integers."));
            return;
        }

        TowerManager.PlacementResult result = gameManager.placeTower(enclave.getIndex(), type, worldX, worldZ);
        if (!result.isOk()) {
            ctx.sendMessage(Message.raw("[TD] Placement failed: " + result.getMessage()));
            return;
        }

        ctx.sendMessage(Message.raw("[TD] Placed " + type.name() + " at (" + worldX + "," + worldZ + ")."));
    }

    private void handleSell(CommandContext ctx, Enclave enclave, String[] tokens) {
        if (tokens.length < 3) {
            ctx.sendMessage(Message.raw("[TD] Usage: /tdtower sell <worldX> <worldZ>"));
            return;
        }

        Integer worldX = parseInt(tokens, 1);
        Integer worldZ = parseInt(tokens, 2);
        if (worldX == null || worldZ == null) {
            ctx.sendMessage(Message.raw("[TD] worldX/worldZ must be integers."));
            return;
        }

        TowerManager.ActionResult result = gameManager.sellTower(enclave.getIndex(), worldX, worldZ);
        ctx.sendMessage(Message.raw("[TD] " + result.getMessage()));
    }

    private void handleUpgrade(CommandContext ctx, Enclave enclave, String[] tokens) {
        if (tokens.length < 4) {
            ctx.sendMessage(Message.raw("[TD] Usage: /tdtower upgrade <worldX> <worldZ> <A|B>"));
            return;
        }

        Integer worldX = parseInt(tokens, 1);
        Integer worldZ = parseInt(tokens, 2);
        if (worldX == null || worldZ == null) {
            ctx.sendMessage(Message.raw("[TD] worldX/worldZ must be integers."));
            return;
        }

        UpgradePath path = parsePath(tokens[3]);
        if (path == null) {
            ctx.sendMessage(Message.raw("[TD] Upgrade path must be A or B."));
            return;
        }

        TowerManager.ActionResult result = gameManager.upgradeTower(enclave.getIndex(), worldX, worldZ, path);
        ctx.sendMessage(Message.raw("[TD] " + result.getMessage()));
    }

    private void sendStatus(CommandContext ctx, Enclave enclave) {
        ctx.sendMessage(Message.raw("[TD] Enclave " + enclave.getIndex()
                + " | lives=" + enclave.getLives()
                + " | gold=" + enclave.getGold()));
    }

    private void sendUsage(CommandContext ctx) {
        ctx.sendMessage(Message.raw("[TD] Usage:"));
        ctx.sendMessage(Message.raw("[TD] /tdtower status"));
        ctx.sendMessage(Message.raw("[TD] /tdtower place <type|wc3UnitId> <worldX> <worldZ>"));
        ctx.sendMessage(Message.raw("[TD] /tdtower sell <worldX> <worldZ>"));
        ctx.sendMessage(Message.raw("[TD] /tdtower upgrade <worldX> <worldZ> <A|B>"));
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
        if ("/tdtower".equalsIgnoreCase(first) || "tdtower".equalsIgnoreCase(first)) {
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

    private UpgradePath parsePath(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("A".equals(normalized) || "PATH_A".equals(normalized)) {
            return UpgradePath.PATH_A;
        }
        if ("B".equals(normalized) || "PATH_B".equals(normalized)) {
            return UpgradePath.PATH_B;
        }
        return null;
    }
}
