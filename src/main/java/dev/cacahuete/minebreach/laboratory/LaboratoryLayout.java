package dev.cacahuete.minebreach.laboratory;

import java.io.*;

public class LaboratoryLayout {
    public static final int SIZE = 10;
    public TileType[] tiles;

    public TileType get(int x, int y) {
        return tiles[y * SIZE + x];
    }

    public static LaboratoryLayout fromStream(InputStream stream) throws IOException {
        LaboratoryLayout layout = new LaboratoryLayout(SIZE, SIZE);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String line = reader.readLine();
        int y = 0;
        while (line != null) {
            String[] tokens = line.replace("-", "").split(",", -1);

            int x = 0;
            for (String token : tokens) {
                layout.tiles[y * SIZE + x] = TileType.Empty;
                for (TileType type : TileType.values()) {
                    if (type.getCsvId().equals(token))
                        layout.tiles[y * SIZE + x] = type;
                }

                x++;
            }

            line = reader.readLine();
            y++;
        }

        return layout;
    }

    public LaboratoryLayout(int width, int height) {
        tiles = new TileType[width * height];
    }

    public enum TileType {
        Empty("", ""),
        StraightVertical("SV", "straight"),
        StraightHorizontal("SH", "straight_h"),
        CornerBottomLeft("BL", "corner_bl"),
        CornerBottomRight("BR", "corner_br"),
        CornerTopLeft("TL", "corner_tl"),
        CornerTopRight("TR", "corner_tr"),
        Plus("P", "plus"),
        TLeft("3L", "t_left"),
        TRight("3R", "t_right"),
        TTop("3T", "t_top"),
        TBottom("3B", "t_bottom"),
        UtilityRoom("U", "utility_room"),
        Staircase("X", "staircase"),
        UpgradeRoom("UPG", "upgrade_room"),
        NitwitSpawn("N", "nitwit_spawn"),
        LibrarianSpawn("L", "librarian_spawn"),
        ;

        String csvId;
        String structureName;

        public String getCsvId() {
            return csvId;
        }

        public String getStructureName() {
            return structureName;
        }

        TileType(String csvId, String structureName) {
            this.csvId = csvId;
            this.structureName = structureName;
        }
    }
}
