package dev.duckslock.enclave;

import dev.duckslock.grid.*;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Runtime model for one player's enclave.
 */
public class Enclave {

    private final int index;
    private final EnclaveColor color;
    private final int worldStartX;
    private final int worldStartZ;
    private final int gridStartX;
    private final int gridStartZ;
    private final int gridWidth;
    private final int gridHeight;
    private final GridSquare[][] grid;
    private final int startingLives;
    private final int startingGold;
    private final double startingInterestPercent;

    private int lives;
    private int gold;
    private double interestPercent;
    private int availableElementPicks;

    @Nullable
    private UUID ownerUuid;

    @Nullable
    private String ownerName;

    public Enclave(
            int index,
            EnclaveColor color,
            int startingLives,
            int startingGold,
            double startingInterestPercent,
            MapDefinition activeMap
    ) {
        this.index = index;
        this.color = color;
        this.startingLives = Math.max(1, startingLives);
        this.startingGold = Math.max(0, startingGold);
        this.startingInterestPercent = Math.max(0d, startingInterestPercent);
        this.lives = this.startingLives;
        this.gold = this.startingGold;
        this.interestPercent = this.startingInterestPercent;
        this.availableElementPicks = 0;

        int column = index % ArenaConstants.ENCLAVES_PER_ROW;
        int row = index / ArenaConstants.ENCLAVES_PER_ROW;

        int regionGridWidth = (int) Math.ceil(activeMap.getGridWidth() / (double) ArenaConstants.ENCLAVES_PER_ROW);
        int regionGridHeight = (int) Math.ceil(activeMap.getGridHeight() / (double) ArenaConstants.ENCLAVE_ROWS);

        this.gridStartX = column * regionGridWidth;
        this.gridStartZ = row * regionGridHeight;
        this.gridWidth = Math.max(1, Math.min(regionGridWidth, activeMap.getGridWidth() - gridStartX));
        this.gridHeight = Math.max(1, Math.min(regionGridHeight, activeMap.getGridHeight() - gridStartZ));

        this.worldStartX = ArenaConstants.ARENA_ORIGIN_X + gridStartX * ArenaConstants.SQUARE_SIZE;
        this.worldStartZ = ArenaConstants.ARENA_ORIGIN_Z + gridStartZ * ArenaConstants.SQUARE_SIZE;
        this.grid = new GridSquare[gridWidth][gridHeight];

        Map<Long, GridSquareType> mapTypes = buildTypeLookup(activeMap.getSquares());
        for (int gx = 0; gx < gridWidth; gx++) {
            for (int gz = 0; gz < gridHeight; gz++) {
                int globalX = gridStartX + gx;
                int globalZ = gridStartZ + gz;
                int worldX = ArenaConstants.ARENA_ORIGIN_X + globalX * ArenaConstants.SQUARE_SIZE;
                int worldZ = ArenaConstants.ARENA_ORIGIN_Z + globalZ * ArenaConstants.SQUARE_SIZE;
                GridSquareType type = mapTypes.getOrDefault(key(globalX, globalZ), GridSquareType.BLOCKED);
                grid[gx][gz] = new GridSquare(gx, gz, worldX, worldZ, type, index);
            }
        }
    }

    public boolean isOwned() {
        return ownerUuid != null;
    }

    public void assignOwner(UUID uuid, String name) {
        this.ownerUuid = uuid;
        this.ownerName = name;
    }

    public void clearOwner() {
        this.ownerUuid = null;
        this.ownerName = null;
    }

    public synchronized boolean deductLives(int amount) {
        if (amount <= 0) {
            return lives <= 0;
        }

        lives = Math.max(0, lives - amount);
        return lives <= 0;
    }

    public synchronized void addGold(int amount) {
        if (amount <= 0) {
            return;
        }
        gold += amount;
    }

    public synchronized boolean spendGold(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (gold < amount) {
            return false;
        }
        gold -= amount;
        return true;
    }

    public synchronized void resetEconomyAndLives() {
        lives = startingLives;
        gold = startingGold;
        interestPercent = startingInterestPercent;
        availableElementPicks = 0;
    }

    public synchronized int payoutInterest() {
        if (interestPercent <= 0d || gold <= 0) {
            return 0;
        }
        int gain = (int) Math.floor(gold * (interestPercent / 100.0d));
        if (gain > 0) {
            gold += gain;
        }
        return gain;
    }

    public synchronized void increaseInterest(double deltaPercent) {
        if (deltaPercent <= 0d) {
            return;
        }
        interestPercent += deltaPercent;
    }

    public synchronized double getInterestPercent() {
        return interestPercent;
    }

    public synchronized void addElementPickToken() {
        availableElementPicks++;
    }

    public synchronized boolean consumeElementPickToken() {
        if (availableElementPicks <= 0) {
            return false;
        }
        availableElementPicks--;
        return true;
    }

    public synchronized int getAvailableElementPicks() {
        return availableElementPicks;
    }

    @Nullable
    public GridSquare getSquare(int gx, int gz) {
        if (gx < 0 || gz < 0 || gx >= gridWidth || gz >= gridHeight) {
            return null;
        }
        return grid[gx][gz];
    }

    @Nullable
    public GridSquare getSquareAtWorldPos(int worldX, int worldZ) {
        int relativeX = worldX - worldStartX;
        int relativeZ = worldZ - worldStartZ;
        if (relativeX < 0 || relativeZ < 0) {
            return null;
        }

        int gx = relativeX / ArenaConstants.SQUARE_SIZE;
        int gz = relativeZ / ArenaConstants.SQUARE_SIZE;
        return getSquare(gx, gz);
    }

    public List<GridSquare> getAllSquares() {
        List<GridSquare> result = new ArrayList<>(gridWidth * gridHeight);

        for (GridSquare[] column : grid) {
            for (GridSquare square : column) {
                result.add(square);
            }
        }

        return result;
    }

    public List<GridSquare> getFreeBuildableSquares() {
        List<GridSquare> result = new ArrayList<>(gridWidth * gridHeight);

        for (GridSquare[] column : grid) {
            for (GridSquare square : column) {
                if (square.isBuildable()) {
                    result.add(square);
                }
            }
        }

        return result;
    }

    public int getCentreWorldX() {
        return worldStartX + (gridWidth * ArenaConstants.SQUARE_SIZE) / 2;
    }

    public int getCentreWorldZ() {
        return worldStartZ + (gridHeight * ArenaConstants.SQUARE_SIZE) / 2;
    }

    public int getIndex() {
        return index;
    }

    public EnclaveColor getColor() {
        return color;
    }

    public int getWorldStartX() {
        return worldStartX;
    }

    public int getWorldStartZ() {
        return worldStartZ;
    }

    public GridSquare[][] getGrid() {
        return grid;
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public int getGridStartX() {
        return gridStartX;
    }

    public int getGridStartZ() {
        return gridStartZ;
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    @Nullable
    public String getOwnerName() {
        return ownerName;
    }

    public synchronized int getLives() {
        return lives;
    }

    public synchronized int getGold() {
        return gold;
    }

    @Override
    public String toString() {
        return "Enclave{index=" + index + " color=" + color.getDisplayName()
                + " owner=" + (ownerName != null ? ownerName : "none") + "}";
    }

    private Map<Long, GridSquareType> buildTypeLookup(List<GridSquareData> squares) {
        Map<Long, GridSquareType> lookup = new HashMap<>();
        for (GridSquareData square : squares) {
            if (square == null) {
                continue;
            }
            GridSquareType type = square.getType() == null ? GridSquareType.BLOCKED : square.getType();
            lookup.put(key(square.getGridX(), square.getGridZ()), type);
        }
        return lookup;
    }

    private long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
