#extension GL_OES_EGL_image_external : require

precision mediump float;       	// Set the default precision to medium. We don't need as high of a
								// precision in the fragment shader.

uniform samplerExternalOES u_Texture;    // The input texture.
//uniform sampler2D u_Texture;    // The input texture.

  
varying vec3 v_Position;		// Interpolated position for this fragment.
  								// triangle per fragment.
varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.
  
// The entry point for our fragment shader.
void main()                    		
{
	// Multiply the color by the diffuse illumination level and texture value to get final output color.
    gl_FragColor = texture2D(u_Texture, v_TexCoordinate);
    //gl_FragColor = vec4(v_TexCoordinate.x, v_TexCoordinate.y, 0, 1);
}                                                                     	

