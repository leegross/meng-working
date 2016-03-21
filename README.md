# meng-working

The main files of the project are:
- TouchControllerActivity.java
- MyGLSurfaceView.java
- MyGLRenderer.java
- DroneWrapper.java
- Hemisphere.java
- Constants.java

TouchControllerActivity is the controller of the project and is where the drone wrapper and the surfaceview are instantiated

MyGLSurfaceView handles all of the touch gestures and it also instantiates the renderer. For a more detailed description of the computations and their derivations, refer to the appendix section in the thesis (Multi-Touch Through-the-Lens Drone Control)

MyGLRenderer creates the openGL world and also represents the projector and camera (which represent the location/orientation of the drone and the current view)

The DroneWrapper is the interface between DJI API and the project. It handles most of the drone controls

Hemisphere creates the hemisphere in openGL

Constants contains all of the constants for the project. From there you can change the sizes of the screen, whether the image projected on the hemisphere is a constant image or the live feed from the drone, as well as other variables. 

Note: sometimes the flight status values on the bottom right panel fail to update upon app start. If this happens, hit the back button and re-enter the live-feed view.
