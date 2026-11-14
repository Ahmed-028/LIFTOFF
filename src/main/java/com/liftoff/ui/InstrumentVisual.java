package com.liftoff.ui;

import com.liftoff.model.Instrument;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;

import java.util.Map;


final class InstrumentVisual {
    private record Host(String file, String name) {}

    private static final Host TRMM = new Host("trmm", "TRMM");
    private static final Host TERRA = new Host("terra", "Terra");
    private static final Host AQUA = new Host("aqua", "Aqua");
    private static final Host LRO = new Host("lro", "Lunar Reconnaissance Orbiter");
    private static final Host ROVER = new Host("perseverance", "Perseverance");

    private static final Map<String, Host> HOSTS = Map.ofEntries(
            Map.entry("ali", new Host("eo1", "Earth Observing-1")),
            Map.entry("etm-plus", new Host("landsat7", "Landsat 7")),
            Map.entry("aster", TERRA), Map.entry("modis", TERRA), Map.entry("ceres", TERRA),
            Map.entry("amsr-e", AQUA), Map.entry("tmi", TRMM), Map.entry("virs", TRMM),
            Map.entry("lis", TRMM), Map.entry("glas", new Host("icesat", "ICESat")),
            Map.entry("lola", LRO), Map.entry("diviner", LRO),
            Map.entry("mastcam-z", ROVER), Map.entry("supercam", ROVER),
            Map.entry("pixl", ROVER), Map.entry("rimfax", ROVER), Map.entry("meda", ROVER));

    private InstrumentVisual() {}

    static String model(Instrument instrument) {
        return switch (instrument.id()) {
            case "mastcam-z" -> "nasa-mastcam";
            case "pixl" -> "nasa-pixl";
            case "rimfax" -> "nasa-rimfax";
            default -> null;
        };
    }

    static String hostName(Instrument instrument) {
        var host = HOSTS.get(instrument.id());
        return host == null ? instrument.flewOn() : host.name();
    }

    static Node hostImage(Instrument instrument) {
        var host = HOSTS.get(instrument.id());
        var file = host == null ? null : InstrumentVisual.class.getResource("/com/liftoff/art/nasa/" + host.file() + ".jpg");
        if (file == null) {
            return null;
        }
        var image = new ImageView(new Image(file.toExternalForm(), 240, 120, true, true));
        image.setAccessibleText(host.name());
        return image;
    }

    static Node diagram(Instrument instrument) {
        var background = new Rectangle(224, 124, Color.web("#081931"));
        background.setArcWidth(16);
        background.setArcHeight(16);
        var grid = new Group();
        for (int x = 16; x < 224; x += 16) {
            grid.getChildren().add(guide(x, 8, x, 116));
        }
        for (int y = 14; y < 124; y += 16) {
            grid.getChildren().add(guide(8, y, 216, y));
        }
        var casework = new Rectangle(47, 25, 115, 73);
        casework.setArcWidth(12);
        casework.setArcHeight(12);
        casework.setFill(Color.web("#28476A"));
        casework.setStroke(Color.web("#72B5D6"));
        var details = switch (instrument.id()) {
            case "tmi", "amsr-e" -> new Group(lens(103, 57, 28), lens(103, 57, 6), guide(103, 57, 145, 31));
            case "meda", "apollo-psep" -> new Group(guide(106, 88, 106, 34), guide(76, 44, 137, 44),
                    lens(76, 44, 7), lens(137, 44, 7), lens(106, 34, 8));
            case "rimfax", "ceres" -> new Group(lens(78, 61, 13), lens(107, 61, 13), lens(136, 61, 13));
            case "lola", "glas", "pixl" -> new Group(lens(76, 51, 12), lens(121, 66, 22), lens(121, 66, 8));
            case "mastcam-z", "aster", "diviner" -> new Group(lens(82, 61, 18), lens(127, 61, 18));
            default -> new Group(lens(102, 59, 25), lens(102, 59, 15), lens(102, 59, 6));
        };
        var bolt = Color.web("#8BCADB");
        return new Group(background, grid, casework, details, new Circle(56, 35, 2.5, bolt),
                new Circle(56, 88, 2.5, bolt), new Circle(152, 35, 2.5, bolt), new Circle(152, 88, 2.5, bolt));
    }

    private static Line guide(double x1, double y1, double x2, double y2) {
        var line = new Line(x1, y1, x2, y2);
        line.setStroke(Color.web("#173654"));
        return line;
    }

    private static Circle lens(double x, double y, double radius) {
        var circle = new Circle(x, y, radius, Color.web("#102B44"));
        circle.setStroke(Color.web("#57DFEB"));
        circle.setStrokeWidth(1.6);
        return circle;
    }
}
