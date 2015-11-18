#extension GL_OES_EGL_image_external : require

precision mediump float;       	// Set the default precision to medium. We don't need as high of a
								// precision in the fragment shader.

uniform sampler2D u_Texture;    // The input texture.
//uniform samplerExternalOES u_Texture;    // The input texture.

varying vec2 v_Texture;         // The input texture.

uniform mat4 u_MVPMatrix_projector;

varying vec3 v_Position;		// Interpolated position for this fragment.
varying vec4 v_Color;          	// This is the color from the vertex shader interpolated across the
  								// triangle per fragment.
varying vec3 v_Normal;         	// Interpolated normal for this fragment.

// The entry point for our fragment shader.
void main()
{
    //gl_FragColor = v_Color;

    vec4 pic = u_MVPMatrix_projector * vec4(v_Position, 1);
    pic = pic / pic.w;

    if (pic.x > -1.0f && pic.x < 1.0f &&
        pic.y > -1.0f && pic.y < 1.0f &&
        pic.z > -1.0f && pic.z < 1.0f) {
        pic = pic * 0.5 + 0.5f;
        pic.y = 1.0f - pic.y;

        gl_FragColor = texture2D(u_Texture, pic.xy);
    } else {
        gl_FragColor = vec4(0, 0, 0, 1);
    }

    //gl_FragColor = texture2D(u_Texture, v_Texture);
    //gl_FragColor = vec4(v_Texture.x, v_Texture.y, 0, 1);
    //gl_FragColor = vec4(projector_imageplane_coord.xy, 1, 1);
}


