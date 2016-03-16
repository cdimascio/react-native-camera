/**
 * Created by Fabrice Armisen (farmisen@gmail.com) on 1/4/16.
 */

package com.lwansbrough.RCTCamera;

import android.hardware.Camera;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RCTCamera {

    private static final RCTCamera ourInstance = new RCTCamera();
    private final HashMap<Integer, CameraInfoWrapper> _cameraInfos;
    private final HashMap<Integer, Integer> _cameraTypeToIndex;
    private final Map<Number, Camera> _cameras;
    private int _orientation = -1;
    private int _actualDeviceOrientation = 0;

    public static RCTCamera getInstance() {
        return ourInstance;
    }

    public Camera acquireCameraInstance(int type) {
        if (null == _cameras.get(type) && null != _cameraTypeToIndex.get(type)) {
            try {
                Camera camera = Camera.open(_cameraTypeToIndex.get(type));
                _cameras.put(type, camera);
                adjustPreviewLayout(type);
            } catch (Exception e) {
                System.console().printf("acquireCameraInstance: %s", e.getLocalizedMessage());
            }
        }
        return _cameras.get(type);
    }

    public void releaseCameraInstance(int type) {
        if (null != _cameras.get(type)) {
            _cameras.get(type).release();
            _cameras.remove(type);
        }
    }

    public int getPreviewWidth(int type) {
        CameraInfoWrapper cameraInfo = _cameraInfos.get(type);
        if (null == cameraInfo) {
            return 0;
        }
        return cameraInfo.previewWidth;
    }

    public int getPreviewHeight(int type) {
        CameraInfoWrapper cameraInfo = _cameraInfos.get(type);
        if (null == cameraInfo) {
            return 0;
        }
        return cameraInfo.previewHeight;
    }

    public void setOrientation(int orientation) {
        if (_orientation == orientation) {
            return;
        }
        _orientation = orientation;
        adjustPreviewLayout(RCTCameraModule.RCT_CAMERA_TYPE_FRONT);
        adjustPreviewLayout(RCTCameraModule.RCT_CAMERA_TYPE_BACK);
    }

    public void setActualDeviceOrientation(int actualDeviceOrientation) {
        _actualDeviceOrientation = actualDeviceOrientation;
        adjustPreviewLayout(RCTCameraModule.RCT_CAMERA_TYPE_FRONT);
        adjustPreviewLayout(RCTCameraModule.RCT_CAMERA_TYPE_BACK);
    }

    public void setTorchMode(int cameraType, int torchMode) {
        Camera camera = _cameras.get(cameraType);
        if (null == camera) {
            return;
        }

        Camera.Parameters parameters = camera.getParameters();
        String value = parameters.getFlashMode();
        switch (torchMode) {
            case RCTCameraModule.RCT_CAMERA_TORCH_MODE_ON:
                value = Camera.Parameters.FLASH_MODE_TORCH;
                break;
            case RCTCameraModule.RCT_CAMERA_TORCH_MODE_OFF:
                value = Camera.Parameters.FLASH_MODE_OFF;
                break;
        }

        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(value)) {
            parameters.setFlashMode(value);
            camera.setParameters(parameters);
        }
    }

    public void setFlashMode(int cameraType, int flashMode) {
        Camera camera = _cameras.get(cameraType);
        if (null == camera) {
            return;
        }

        Camera.Parameters parameters = camera.getParameters();
        String value = parameters.getFlashMode();
        switch (flashMode) {
            case RCTCameraModule.RCT_CAMERA_FLASH_MODE_AUTO:
                value = Camera.Parameters.FLASH_MODE_AUTO;
                break;
            case RCTCameraModule.RCT_CAMERA_FLASH_MODE_ON:
                value = Camera.Parameters.FLASH_MODE_ON;
                break;
            case RCTCameraModule.RCT_CAMERA_FLASH_MODE_OFF:
                value = Camera.Parameters.FLASH_MODE_OFF;
                break;
        }
        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(value)) {
            parameters.setFlashMode(value);
            camera.setParameters(parameters);
        }
    }

    private void adjustPreviewLayout(int type) {
        Camera camera = _cameras.get(type);
        if (null == camera) {
            return;
        }

        CameraInfoWrapper cameraInfo = _cameraInfos.get(type);
        int displayRotation;
        int rotation;
        int orientation = cameraInfo.info.orientation;
        if (cameraInfo.info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (orientation + _actualDeviceOrientation * 90) % 360;
            displayRotation = (720 - orientation - _actualDeviceOrientation * 90) % 360;
        } else {
            rotation = (orientation - _actualDeviceOrientation * 90 + 360) % 360;
            displayRotation = rotation;
        }
        cameraInfo.rotation = rotation;
        // TODO: take in account the _orientation prop

        camera.setDisplayOrientation(displayRotation);

        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(cameraInfo.rotation);

        // set preview size
        Camera.Size desiredPreviewSize = getDesiredCameraSize(parameters.getSupportedPreviewSizes());
        Camera.Size desiredPictureSize = getDesiredCameraSize(parameters.getSupportedPreviewSizes());

        int previewWidth = desiredPreviewSize.width;
        int previewHeight = desiredPreviewSize.height;

        parameters.setPreviewSize(previewWidth, previewHeight);

        parameters.setPictureSize(desiredPictureSize.width, desiredPictureSize.height);

        try {
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (cameraInfo.rotation == 0 || cameraInfo.rotation == 180) {
            cameraInfo.previewWidth = previewWidth;
            cameraInfo.previewHeight = previewHeight;
        } else {
            cameraInfo.previewWidth = previewHeight;
            cameraInfo.previewHeight = previewWidth;
        }
    }

    private Camera.Size getDesiredCameraSize(List<Camera.Size> sizes) {
        Camera.Size desiredSize = sizes.get(0); // choose best size by default
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long availableMemory = Runtime.getRuntime().maxMemory() - usedMemory;
        // search for desired size i.e. 4:3 ratio
        for (Camera.Size currentSize : sizes) {
            int area = currentSize.width * currentSize.height;
            long neededMemory = area * 4 * 4; // area * 4 Bytes/pixel * 4 needed copies of the bitmap (for safety :) )
            boolean isDesiredRatio = (currentSize.width / 4) == (currentSize.height / 3);
            boolean isBetterQuality = (desiredSize == null || currentSize.width > desiredSize.width);
            boolean isSafe = neededMemory < availableMemory;
            if (isDesiredRatio && isBetterQuality && isSafe) {
                desiredSize = currentSize;
            }
        }
        return desiredSize;

    }

    private RCTCamera() {
        _cameras = new HashMap<>();
        _cameraInfos = new HashMap<>();
        _cameraTypeToIndex = new HashMap<>();

        // map camera types to camera indexes and collect cameras properties
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && _cameraInfos.get(RCTCameraModule.RCT_CAMERA_TYPE_FRONT) == null) {
                _cameraInfos.put(RCTCameraModule.RCT_CAMERA_TYPE_FRONT, new CameraInfoWrapper(info));
                _cameraTypeToIndex.put(RCTCameraModule.RCT_CAMERA_TYPE_FRONT, i);
                acquireCameraInstance(RCTCameraModule.RCT_CAMERA_TYPE_FRONT);
                releaseCameraInstance(RCTCameraModule.RCT_CAMERA_TYPE_FRONT);
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK && _cameraInfos.get(RCTCameraModule.RCT_CAMERA_TYPE_BACK) == null) {
                _cameraInfos.put(RCTCameraModule.RCT_CAMERA_TYPE_BACK, new CameraInfoWrapper(info));
                _cameraTypeToIndex.put(RCTCameraModule.RCT_CAMERA_TYPE_BACK, i);
                acquireCameraInstance(RCTCameraModule.RCT_CAMERA_TYPE_BACK);
                releaseCameraInstance(RCTCameraModule.RCT_CAMERA_TYPE_BACK);
            }
        }
    }

    private class CameraInfoWrapper {
        public final Camera.CameraInfo info;
        public int rotation = 0;
        public int previewWidth = -1;
        public int previewHeight = -1;

        public CameraInfoWrapper(Camera.CameraInfo info) {
            this.info = info;
        }
    }
}
