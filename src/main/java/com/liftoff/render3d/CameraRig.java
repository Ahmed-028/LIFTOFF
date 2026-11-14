package com.liftoff.render3d;

import javafx.scene.PerspectiveCamera;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;


final class CameraRig {
    final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Translate position = new Translate();
    private final Rotate yawRotate = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate pitchRotate = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rollRotate = new Rotate(0, Rotate.Z_AXIS);
    double x, y, z;

    double yaw, pitch, roll;

    private double r00, r01, r02, r10, r11, r12, r20, r21, r22;

    CameraRig() {
        camera.getTransforms().addAll(position, yawRotate, pitchRotate, rollRotate);
        lookAt(0, 0, -10, 0, 0, 0, 0);
    }

    void lookAt(double ex, double ey, double ez, double tx, double ty, double tz, double rollRadians) {
        double dx = tx - ex;
        double dy = ty - ey;
        double dz = tz - ez;
        set(ex, ey, ez, Math.atan2(dx, dz), Math.atan2(-dy, Math.hypot(dx, dz)), rollRadians);
    }

    void set(double ex, double ey, double ez, double yawRadians, double pitchRadians, double rollRadians) {
        x = ex;
        y = ey;
        z = ez;
        yaw = yawRadians;
        pitch = pitchRadians;
        roll = rollRadians;
        position.setX(ex);
        position.setY(ey);
        position.setZ(ez);
        yawRotate.setAngle(Math.toDegrees(yawRadians));
        pitchRotate.setAngle(Math.toDegrees(pitchRadians));
        rollRotate.setAngle(Math.toDegrees(rollRadians));

        double cb = Math.cos(yawRadians), sb = Math.sin(yawRadians);
        double ca = Math.cos(pitchRadians), sa = Math.sin(pitchRadians);
        double cc = Math.cos(rollRadians), sc = Math.sin(rollRadians);

        double m00 = cb, m01 = sb * sa, m02 = sb * ca;
        double m10 = 0, m11 = ca, m12 = -sa;
        double m20 = -sb, m21 = cb * sa, m22 = cb * ca;

        double n00 = m00 * cc + m01 * sc, n01 = -m00 * sc + m01 * cc, n02 = m02;
        double n10 = m10 * cc + m11 * sc, n11 = -m10 * sc + m11 * cc, n12 = m12;
        double n20 = m20 * cc + m21 * sc, n21 = -m20 * sc + m21 * cc, n22 = m22;
        r00 = n00; r01 = n10; r02 = n20;
        r10 = n01; r11 = n11; r12 = n21;
        r20 = n02; r21 = n12; r22 = n22;
    }


    void toCamera(double wx, double wy, double wz, double[] out) {
        out[0] = r00 * wx + r01 * wy + r02 * wz;
        out[1] = r10 * wx + r11 * wy + r12 * wz;
        out[2] = r20 * wx + r21 * wy + r22 * wz;
    }

    double fieldOfView() {
        return camera.getFieldOfView();
    }
}
