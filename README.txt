Jacob Chappell and Bryan Potere
12/12/14

Readme file for CS 335 Final Project

INCLUDED FILES

Source code files:

	Camera. java
	Car.java
	JoglEventListener.java
	Main.java
	Skybox.java
	TempBuilder.java
	TextureLoader.java
	TextureLoaderClamping.java
	Vector3.java
	WavefrontParser.java

A "police car" folder containing object files and textures for a model of a police car.

The racetrack_textures folder contains several .jpg files used to texture the game environment.

The skybox_textures folder contains textures used for the skybox and contains its own license and readme file.

SYNOPSIS

	Graphics Environment and Camera Viewpoint

	This java project implements a 3-d racing game using OpenGL.  The player controls a police car and races against other police cars around a half mile elliptical course for three laps.  The player's camera is centered at all times within a skybox large enough to contain the entire environment.  This maintains the illusion of a sky that does not change in an unrealistic manner.  Two viewpoints are available: a free view camera (button 1) and a view behind the wheel of the car that the player controls (button 2).  When in the free-view mode, the player can move around the course with the w, a, s, and d keys and change the viewing angle by clicking and dragging the mouse.  No matter which position or angle that the player uses, the trees in the center of the ellipse always face the camera.  Although they are two-dimensional images, this billboard technique maintains an illusion of a three dimensional object.  The buildings include two jumbotron sized scoreboard pylons that display the player's current lap (starting at one when the player first crosses the checkered line).  Additionally a garage is present for flavor.  Every object in the game envirnoment is textured.

	Controlling the Car

	The car is controllable with the arrow keys: forward to accelerate, back to brake, left and right to move inwards and outwards along the track.  If the player continues to brake, eventually the car moves backwards.  

	Game AI

	The other cars controlled by the computer move somewhat erratically around the course.  At times they will accelerate forwards and brake back, and sway left and right as they move along.  No car can move outside the bounds of the asphalt.  If one car collides with another, both reduce their speed by fifteen percent.  Collision detection is performed in two-dimensions so that cars cannot move through each other.

REFERENCES

  Owens, Sean R.  Parser and Builder.  http://lwjgl.org  27 Oct. 2011.  Web.  12 Dec. 2014.
