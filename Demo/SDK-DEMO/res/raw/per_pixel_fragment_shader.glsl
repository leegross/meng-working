#extension GL_OES_EGL_image_external : require

precision mediump float;       	// Set the default precision to medium. We don't need as high of a
								// precision in the fragment shader.

uniform samplerExternalOES u_Texture;    // The input texture.
//uniform sampler2D u_Texture;    // The input texture.

uniform mat4 u_MVPMatrix_projector;
  
varying vec3 v_Position;		// Interpolated position for this fragment.
  								// triangle per fragment.
varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.
  
// The entry point for our fragment shader.
void main()                    		
{
	// Multiply the color by the diffuse illumination level and texture value to get final output color.
    //gl_FragColor = texture2D(u_Texture, v_TexCoordinate);
    gl_FragColor = vec4(v_TexCoordinate.x, v_TexCoordinate.y, 0, 1);

    vec4 projector_imageplane_coord = u_MVPMatrix_projector * vec4(v_Position, 1);
    projector_imageplane_coord = projector_imageplane_coord / projector_imageplane_coord.w;
    // gl_FragColor = texture2D(u_Texture, projector_imageplane_coord.xy);
}                                                                     	

