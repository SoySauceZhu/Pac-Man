package byog.Core;

import byog.TileEngine.TETile;
import byog.TileEngine.Tileset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*TODO: Replace every TETile[][] argument with instance variable*/

public class Map implements Serializable {
    private final TETile[][] floorTiles;
    private final int WIDTH;
    private final int HEIGHT;

    private Position playerPos;

    private final ArrayList<Position> coinsPosition = new ArrayList<>();

    private final TETile _default = Tileset.FLOOR;

    Random random;

    public Map(int WIDTH, int HEIGHT) {
        this.WIDTH = WIDTH;
        this.HEIGHT = HEIGHT;
        floorTiles = new TETile[WIDTH][HEIGHT];
        fillWithNothing(floorTiles);
        random = new Random();
        playerPos = new Position(0, 0);
    }

    private static class Position implements Serializable {
        protected int X;
        protected int Y;

        public Position(int x, int y) {
            X = x;
            Y = y;
        }

        @Override
        public boolean equals(Object obj) {
            Position pos = (Position) obj;
            return pos.X == this.X && pos.Y == this.Y;
        }
    }

    private static class Room {
        protected int width;
        protected int height;
        protected int X;
        protected int Y;

        public Room(int width, int height, int x, int y) {
            this.width = width;
            this.height = height;
            X = x;
            Y = y;
        }
    }

    public TETile[][] initialize() {

        int roomNum = RandomUtils.uniform(random, 25, 30);

        // List of rooms, recording Height, Width, X and Y parameters of a room
        Room[] roomsList = createRoomScheme(roomNum);

        // Update the floorTiles by filling positions that room list indicates with _default (Tileset.FLOOR)
        drawRooms(roomsList, floorTiles);

        // Update the floorTiles by linking adjacent rooms
        linkRegions(floorTiles);

        drawWalls(floorTiles);

        drawDecoration(floorTiles);

        drawDoor(floorTiles);

        createCoins(floorTiles);

        return floorTiles;
    }

    private void createCoins(TETile[][] floorTiles) {
        ArrayList<Position> list = new ArrayList<>();
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (floorTiles[x][y].description().equals("floor")
                        || floorTiles[x][y].description().equals("grass")) {
                    list.add(new Position(x, y));
                }
            }
        }

        int count = Math.min(list.size(), 15);

        for (int i = 0; i < count; i++) {
            int randomIndex = random.nextInt(list.size());
            coinsPosition.add(list.get(randomIndex));
            list.remove(randomIndex);
        }

    }

    private void drawDoor(TETile[][] floorTiles) {
        for (int x = WIDTH / 3; x < WIDTH * 2 / 3; x++) {
            for (int y = 0; y < HEIGHT / 2; y++) {
                boolean isWall = floorTiles[x][y].description().equals("wall");
                boolean upperValid = floorTiles[x][y + 1].description().equals("floor") || floorTiles[x][y + 1].description().equals("grass");
                boolean lowerValid = y == 0 || floorTiles[x][y - 1].description().equals("nothing");
                if (isWall && upperValid && lowerValid) {
                    floorTiles[x][y] = Tileset.LOCKED_DOOR;
                    playerPos = new Position(x, y + 1);
                    return;
                }
            }
        }
    }

    private void drawDecoration(TETile[][] floorTiles) {
        for (int i = 0; i < floorTiles.length; i++) {
            for (int j = 0; j < floorTiles[0].length; j++) {
                if (floorTiles[i][j].description().equals("floor") && RandomUtils.gaussian(random) > 0.8) {
                    floorTiles[i][j] = Tileset.GRASS;
                }
            }

        }
    }

    private void drawWalls(TETile[][] tiles) {
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[0].length; j++) {
                if (tiles[i][j] == _default) {
                    drawWallAround(new Position(i, j), tiles);
                }
            }
        }
    }

    private void drawWallAround(Position p, TETile[][] tiles) {
        int x = p.X;
        int y = p.Y;

        if (x - 1 >= 0 && tiles[x - 1][y].description().equals("nothing")) {
            tiles[x - 1][y] = Tileset.WALL;
        }

        if (x + 1 < tiles.length && tiles[x + 1][y].description().equals("nothing")) {
            tiles[x + 1][y] = Tileset.WALL;
        }

        if (y - 1 >= 0 && tiles[x][y - 1].description().equals("nothing")) {
            tiles[x][y - 1] = Tileset.WALL;
        }

        if (y + 1 < tiles[0].length && tiles[x][y + 1].description().equals("nothing")) {
            tiles[x][y + 1] = Tileset.WALL;
        }

        if (x - 1 >= 0 && y - 1 >= 0 && tiles[x - 1][y - 1].description().equals("nothing")) {
            tiles[x - 1][y - 1] = Tileset.WALL;
        }

        if (x + 1 < tiles.length && y - 1 >= 0 && tiles[x + 1][y - 1].description().equals("nothing")) {
            tiles[x + 1][y - 1] = Tileset.WALL;
        }

        if (x - 1 >= 0 && y + 1 < tiles[0].length && tiles[x - 1][y + 1].description().equals("nothing")) {
            tiles[x - 1][y + 1] = Tileset.WALL;
        }

        if (x + 1 < tiles.length && y + 1 < tiles[0].length && tiles[x + 1][y + 1].description().equals("nothing")) {
            tiles[x + 1][y + 1] = Tileset.WALL;
        }
    }

    /**
     * Update the input tiles by linking adjacent non-null tiles group
     */
    private void linkRegions(TETile[][] tiles) {
        // UnionMap will only contain small number of region ID. e.g. -1 1 2 3 ... but no 0
        Tuple digitUnionMap = unionMap(tilesToDigitMap(tiles));

        int[][] unionMap = digitUnionMap.unionMap;
        int regionNum = digitUnionMap.regionNum;

        List<Position>[] regions = new List[regionNum];

        for (int i = 0; i < regionNum; i++) {
            regions[i] = new ArrayList<>();
        }

        for (int i = 0; i < unionMap.length; i++) {
            for (int j = 0; j < unionMap[0].length; j++) {
                if (unionMap[i][j] != -1) {
                    regions[unionMap[i][j] - 1].add(new Position(i, j));
                }
            }
        }

        for (int i = 0; i < regionNum - 1; i++) {
            Position p1 = regions[i].get(RandomUtils.uniform(random, regions[i].size()));
            Position p2 = regions[i + 1].get(RandomUtils.uniform(random, regions[i + 1].size()));
            linkTwoPosition(p1, p2, tiles);
        }
    }

    /**
     * Union intersecting rooms and update digitMap, return a Tuple.
     */
    private Tuple unionMap(int[][] digitMap) {
        int[][] unionMap = digitMap;

        int numOfRegions = 0;

        for (int i = 0; i < digitMap.length; i++) {
            for (int j = 0; j < digitMap[0].length; j++) {
                if (digitMap[i][j] == 0) {
                    numOfRegions++;
                    floodFill(unionMap, i, j, 0, numOfRegions);
                }
            }
        }

        return new Tuple(unionMap, numOfRegions);
    }

    private class Tuple {
        int[][] unionMap;
        int regionNum;

        public Tuple(int[][] unionMap, int regionNum) {
            this.unionMap = unionMap;
            this.regionNum = regionNum;
        }
    }

    /**
     * Update digit map. Find and replace all adjacent source-like tile by recursion
     */
    private void floodFill(int[][] map, int source_x, int source_y, int source, int target) {
        // If the ptr is out of bound, no replacement
        if (source_x < 0 || source_x >= WIDTH - 1 || source_y < 0 || source_y >= HEIGHT - 1) return;
        // If the value on ptr is not empty (0), no replacement
        if (map[source_x][source_y] != source) return;

        map[source_x][source_y] = target;

        floodFill(map, source_x + 1, source_y, source, target);
        floodFill(map, source_x, source_y + 1, source, target);
        floodFill(map, source_x - 1, source_y, source, target);
        floodFill(map, source_x, source_y - 1, source, target);
    }


    /**
     * @return Return int[][] matrix where -1 means nothing, 0 means floor
     */
    private int[][] tilesToDigitMap(TETile[][] tiles) {
        int[][] digitMap = new int[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (tiles[x][y].description().equals("nothing")) {
                    digitMap[x][y] = -1;        // -1 means nothing
                } else {
                    digitMap[x][y] = 0;         // 0 means unchecked floor
                }
            }
        }
        return digitMap;
    }

    /**
     * Update the map-tiles by linking two position
     */
    private void linkTwoPosition(Position A, Position B, TETile[][] tiles) {
        if (B.Y >= A.Y) {
            for (int i = A.Y; i <= B.Y; i++) {
                tiles[A.X][i] = _default;
            }
        } else {
            for (int i = B.Y; i <= A.Y; i++) {
                tiles[A.X][i] = _default;
            }
        }

        if (B.X >= A.X) {
            for (int i = A.X; i <= B.X; i++) {
                tiles[i][B.Y] = _default;
            }
        } else {
            for (int i = B.X; i <= A.X; i++) {
                tiles[i][B.Y] = _default;
            }
        }


    }


    private void fillWithNothing(TETile[][] tiles) {
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                if (tiles[i][j] == null) {
                    tiles[i][j] = Tileset.NOTHING;
                }
            }
        }
    }

    /**
     * @return Return a list of room instances
     */
    private Room[] createRoomScheme(int roomNum) {
        Room[] rooms = new Room[roomNum];

        for (int i = 0; i < roomNum; i++) {
            int randX = RandomUtils.uniform(random, 1, WIDTH - 11);
            int randY = RandomUtils.uniform(random, 1, HEIGHT - 11);
            int randWidth = RandomUtils.uniform(random, 2, 10);
            int randHeight = RandomUtils.uniform(random, 2, 10);

            rooms[i] = new Room(randWidth, randHeight, randX, randY);
        }

        return rooms;
    }


    /**
     * Update the tiles by adding rooms in Room[]
     */
    private void drawRooms(Room[] rooms, TETile[][] tiles) {
        for (Room room : rooms) {
            for (int x = room.X; x < room.width + room.X; x++) {
                for (int y = room.Y; y < room.height + room.Y; y++) {
                    tiles[x][y] = _default;
                }
            }
        }
    }

    public void setRandom(long seed) {
        this.random = new Random(seed);
    }

    public void control(char step) {
        int x = playerPos.X;
        int y = playerPos.Y;
        if (step == 'w' || step == 'W' || step == 'k') {
            if (y < HEIGHT - 1 && !floorTiles[x][y + 1].description().equals("wall")) {
                playerPos.Y++;
            }
        }

        if (step == 'a' || step == 'A' || step == 'h') {
            if (x > 0 && !floorTiles[x - 1][y].description().equals("wall")) {
                playerPos.X--;
            }
        }

        if (step == 's' || step == 'S' || step == 'j') {
            if (y > 0 && !floorTiles[x][y - 1].description().equals("wall")) {
                playerPos.Y--;
            }
        }

        if (step == 'd' || step == 'D' || step == 'l') {
            if (x < WIDTH - 1 && !floorTiles[x + 1][y].description().equals("wall")) {
                playerPos.X++;
            }
        }

        for (int i = 0; i < coinsPosition.size(); i++) {
            if (coinsPosition.get(i).equals(playerPos)) {
                coinsPosition.remove(i);
            }
        }
    }

    public void ninjaControl(char step) {
        int x = playerPos.X;
        int y = playerPos.Y;
        if (step == 'w' || step == 'W' || step == 'k') {
            while (y < HEIGHT - 1 && !floorTiles[x][y + 1].description().equals("wall")) {
                y++;
                playerPos.Y++;
            }
        }

        if (step == 'a' || step == 'A' || step == 'h') {
            while (x > 0 && !floorTiles[x - 1][y].description().equals("wall")) {
                x--;
                playerPos.X--;
            }
        }

        if (step == 's' || step == 'S' || step == 'j') {
            while (y > 0 && !floorTiles[x][y - 1].description().equals("wall")) {
                y--;
                playerPos.Y--;
            }
        }

        if (step == 'd' || step == 'D' || step == 'l') {
            while (x < WIDTH - 1 && !floorTiles[x + 1][y].description().equals("wall")) {
                x++;
                playerPos.X++;
            }
        }
    }

    public TETile[][] getFloorTiles() {
        TETile[][] output = new TETile[WIDTH][HEIGHT];

        // Copy
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                output[x][y] = floorTiles[x][y];
            }
        }

        for (Position pos : coinsPosition) {
            int x = pos.X;
            int y = pos.Y;
            output[x][y] = Tileset.COIN;
        }


        int x = playerPos.X;
        int y = playerPos.Y;
        output[x][y] = Tileset.PLAYER;
        return output;
    }
}
