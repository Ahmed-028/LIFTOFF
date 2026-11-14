package com.liftoff.render3d;

import javafx.scene.Group;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * A simple blocky character for the cutscenes, built only from boxes like the cockpit pilot:
 * a box head with a drawn-on face, a box body, and arms and legs that swing at the shoulders and
 * hips. Nova wears space goggles pushed up on the forehead. The figure faces -Z (towards a
 * default camera) and stands on y = 0.
 */
final class RoundFigure extends Group {

    /** What the figure looks like. */
    record Look(boolean grownUp, String suit, String trim, String legs, String skin, String hair, boolean goggles) {
        static final Look KID_NOVA = new Look(false, "#F2F5FA", "#FF7A1A", "#DCE8F7", "#F6C9A0", "#5A3A2A", true);
        static final Look GROWN_UP_NOVA = new Look(true, "#16305E", "#FF7A1A", "#0D1E42", "#F6C9A0", "#5A3A2A", true);
        static final Look PRESENTER = new Look(true, "#5B3A8C", "#FFCB69", "#2A1C40", "#8D5A3B", "#1A120C", false);
    }

    final Group body = new Group();
    private final Group leftArm = new Group();
    private final Group rightArm = new Group();
    private final Group leftLeg = new Group();
    private final Group rightLeg = new Group();
    private final Rotate leftArmSwing = new Rotate(0, Rotate.X_AXIS);
    private final Rotate leftArmRaise = new Rotate(0, Rotate.Z_AXIS);
    private final Rotate rightArmSwing = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rightArmRaise = new Rotate(0, Rotate.Z_AXIS);
    private final Rotate leftLegSwing = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rightLegSwing = new Rotate(0, Rotate.X_AXIS);
    private final double shoulderY;
    private final double armLength;
    private final double headY;

    RoundFigure(Look look) {
        // Everyone has Nova's shape; grown-ups are the same figure drawn a little bigger,
        // in a jacket instead of a space suit.
        boolean big = look.grownUp();
        double legLength = .74;
        double torsoHeight = .95;
        double torsoWidth = .9;
        double torsoDepth = .55;
        double headSize = .78;
        armLength = .78;
        double hipY = -legLength;
        double torsoTop = hipY - torsoHeight;
        shoulderY = torsoTop + .12;
        headY = torsoTop - .06 - headSize / 2;

        // Legs, hinged at the hips, with chunky boots.
        for (int side : new int[] {-1, 1}) {
            var leg = side < 0 ? leftLeg : rightLeg;
            var swing = side < 0 ? leftLegSwing : rightLegSwing;
            leg.getTransforms().setAll(new Translate(side * torsoWidth * .25, hipY, 0), swing);
            leg.getChildren().addAll(
                    box(.3, legLength - .14, .32, 0, (legLength - .14) / 2, 0, look.legs()),
                    box(.34, .16, .46, 0, legLength - .08, -.06, big ? "#0B1A38" : "#16305E"));
            getChildren().add(leg);
        }

        // Body: torso, belt stripe, chest badge and either a backpack or a jacket stripe and collar.
        body.getChildren().addAll(
                box(torsoWidth, torsoHeight, torsoDepth, 0, hipY - torsoHeight / 2, 0, look.suit()),
                box(torsoWidth + .03, .13, torsoDepth + .03, 0, hipY - .16, 0, look.trim()),
                glow(.17, .17, .04, "#22E3F2", torsoWidth * .22, torsoTop + torsoHeight * .3, -torsoDepth / 2 - .02));
        if (big) {
            body.getChildren().addAll(
                    box(.1, torsoHeight * .78, .04, -torsoWidth * .18, torsoTop + torsoHeight * .45, -torsoDepth / 2 - .02, look.trim()),
                    box(torsoWidth * .62, .12, torsoDepth + .04, 0, torsoTop + .04, 0, "#F2F5FA"));
        } else {
            body.getChildren().add(box(.62, .66, .26, 0, torsoTop + .46, torsoDepth / 2 + .13, look.trim()));
        }

        // Arms, hinged at the shoulders: sleeve, cuff and a hand (white gloves for young Nova).
        for (int side : new int[] {-1, 1}) {
            var arm = side < 0 ? leftArm : rightArm;
            var swing = side < 0 ? leftArmSwing : rightArmSwing;
            var raise = side < 0 ? leftArmRaise : rightArmRaise;
            arm.getTransforms().setAll(new Translate(side * (torsoWidth / 2 + .13), shoulderY, 0), raise, swing);
            double sleeve = armLength - .2;
            arm.getChildren().addAll(
                    box(.24, sleeve, .26, 0, sleeve / 2, 0, look.suit()),
                    box(.27, .06, .29, 0, sleeve - .03, 0, look.trim()),
                    box(.27, .22, .27, 0, sleeve + .11, 0, big ? look.skin() : "#F2F5FA"));
            body.getChildren().add(arm);
        }

        // Head with a simple face.
        double front = -headSize * .45 - .01;
        body.getChildren().addAll(
                box(.24, .1, .24, 0, torsoTop - .03, 0, look.skin()),
                box(headSize, headSize * .95, headSize * .9, 0, headY, 0, look.skin()));
        for (int side : new int[] {-1, 1}) {
            body.getChildren().addAll(
                    box(.1, .15, .03, side * .16, headY - .02, front, "#0B1A38"),
                    glow(.035, .04, .03, "#FFFFFF", side * .16 + .025, headY - .06, front - .01),
                    box(.12, .07, .02, side * .27, headY + .1, front, "#FF9DA8"),
                    box(.06, .2, .2, side * (headSize / 2 + .03), headY + .02, 0, look.skin()));
        }
        // Smile: a short bar with two raised corners.
        body.getChildren().addAll(
                box(.16, .04, .03, 0, headY + .17, front, "#7A2E2E"),
                box(.05, .04, .03, -.1, headY + .14, front, "#7A2E2E"),
                box(.05, .04, .03, .1, headY + .14, front, "#7A2E2E"));
        // Hair: a block on top, the back and a fringe.
        double top = headY - headSize * .475;
        body.getChildren().addAll(
                box(headSize + .04, .16, headSize * .9 + .04, 0, top - .02, 0, look.hair()),
                box(headSize + .04, headSize * .55, .12, 0, headY - .08, headSize * .45 - .02, look.hair()),
                box(headSize * .5, .1, .06, -headSize * .18, top + .09, front, look.hair()));
        if (look.goggles()) {
            addGoggles(headSize, top, front);
        }

        getChildren().add(body);
        // Grown-ups are scaled about the feet, so they still stand on y = 0 whatever their arms do.
        scale = big ? GROWN_UP : 1;
        if (big) {
            getTransforms().add(new javafx.scene.transform.Scale(scale, scale, scale, 0, 0, 0));
        }
        hands(0, 0);
    }

    private static final double GROWN_UP = 1.18;
    private double scale;

    /** Space goggles pushed up on the forehead: an orange strap and two glowing cyan lenses. */
    private void addGoggles(double headSize, double top, double front) {
        double y = top + .17;
        body.getChildren().add(box(headSize + .07, .1, headSize * .9 + .07, 0, y, 0, "#FF7A1A"));
        for (int side : new int[] {-1, 1}) {
            body.getChildren().addAll(
                    box(.24, .2, .07, side * .15, y - .02, front - .04, "#16305E"),
                    glow(.17, .13, .03, "#22E3F2", side * .15, y - .02, front - .085));
        }
        body.getChildren().add(box(.08, .06, .06, 0, y - .02, front - .04, "#16305E"));
    }

    /**
     * Arms by the sides; {@code swing} moves them back and forth (walking), {@code reach} lifts
     * them forward to work at a desk or hold something.
     */
    void hands(double swing, double reach) {
        leftArmRaise.setAngle(6);
        rightArmRaise.setAngle(-6);
        leftArmSwing.setAngle(swing * 110 - 72 * reach);
        rightArmSwing.setAngle(-swing * 110 - 72 * reach);
    }

    /** Small tapping movements while reaching forward to a keyboard. */
    void type(double t) {
        leftArmSwing.setAngle(-72 + Math.sin(t * 22) * 4);
        rightArmSwing.setAngle(-72 - Math.sin(t * 22) * 4);
    }

    /** The right arm up beside the head, waving. */
    void wave(double t) {
        rightArmSwing.setAngle(0);
        rightArmRaise.setAngle(-150 + Math.sin(t * 9) * 16);
    }

    /** The right arm held straight out to the side, pointing. */
    void point() {
        rightArmSwing.setAngle(-20);
        rightArmRaise.setAngle(-92);
    }

    /** Both arms straight up, holding something high. */
    void cheer(double t) {
        double bounce = Math.sin(t * 6) * 3;
        leftArmSwing.setAngle(-14);
        rightArmSwing.setAngle(-14);
        leftArmRaise.setAngle(166 + bounce);
        rightArmRaise.setAngle(-166 - bounce);
    }

    /** How high the hands are when cheering, measured from the feet, after any scaling. */
    double handsHighY() {
        return (shoulderY - armLength * .95) * scale;
    }

    /** A walking step; standing still when {@code walking} is false. */
    void bob(double t, boolean walking) {
        double stride = walking ? Math.sin(t * 9) : 0;
        body.setTranslateY(walking ? -Math.abs(stride) * .06 : 0);
        leftLegSwing.setAngle(stride * 28);
        rightLegSwing.setAngle(-stride * 28);
        hands(stride * .22, 0);
    }

    private static Box box(double w, double h, double d, double x, double y, double z, String colour) {
        var box = new Box(w, h, d);
        box.setMaterial(ModelLibrary.surface(colour));
        return Meshes.at(box, x, y, z);
    }

    private static Box glow(double w, double h, double d, String colour, double x, double y, double z) {
        var box = new Box(w, h, d);
        box.setMaterial(ModelLibrary.lit(colour, colour));
        return Meshes.at(box, x, y, z);
    }
}
