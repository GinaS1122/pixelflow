/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java.dwgl;


import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Stack;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;

public class DwGLSLProgram {
  
  public DwPixelFlow context;
  public GL2ES2 gl;
  public int HANDLE;

  public DwGLSLShader vert;
  public DwGLSLShader geom;
  public DwGLSLShader frag;
 
  public String name;
  
  public DwGLSLProgram(DwPixelFlow context, String vert_path, String geom_path, String frag_path) {
    this.context = context;
    this.gl = context.gl;
    
    this.vert = new DwGLSLShader(context, GL3.GL_VERTEX_SHADER  , vert_path);
    this.geom = new DwGLSLShader(context, GL3.GL_GEOMETRY_SHADER, geom_path);
    this.frag = new DwGLSLShader(context, GL3.GL_FRAGMENT_SHADER, frag_path);
    this.name = vert.path+"/"+geom.path+"/"+frag.path;
  }
  
  public DwGLSLProgram(DwPixelFlow context, String vert_path, String frag_path) {
    this.context = context;
    this.gl = context.gl;
    
    this.vert = new DwGLSLShader(context, GL2ES2.GL_VERTEX_SHADER  , vert_path);
    this.frag = new DwGLSLShader(context, GL2ES2.GL_FRAGMENT_SHADER, frag_path);
    this.name = vert.path+"/"+frag.path;
  }

  public void release(){
    if(vert != null) vert.release();
    if(frag != null) frag.release();
    if(geom != null) geom.release();
    gl.glDeleteProgram(HANDLE); HANDLE = 0;
  }
  
  private boolean build(DwGLSLShader shader){
    return (shader != null) && shader.build();
  }
  
  public DwGLSLProgram build() {
    if((build(vert) | build(geom) | build(frag)) || (HANDLE == 0)){
     
      if(HANDLE == 0){
        HANDLE = gl.glCreateProgram();
      } else {
        if(vert != null) gl.glDetachShader(HANDLE, vert.HANDLE);
        if(geom != null) gl.glDetachShader(HANDLE, geom.HANDLE);
        if(frag != null) gl.glDetachShader(HANDLE, frag.HANDLE);
      }
      
      if(vert != null) gl.glAttachShader(HANDLE, vert.HANDLE);  DwGLError.debug(gl, "DwGLSLProgram.build  1");
      if(geom != null) gl.glAttachShader(HANDLE, geom.HANDLE);  DwGLError.debug(gl, "DwGLSLProgram.build 2");
      if(frag != null) gl.glAttachShader(HANDLE, frag.HANDLE);  DwGLError.debug(gl, "DwGLSLProgram.build 3");
      
      gl.glLinkProgram(HANDLE);
      
  //    gl.glValidateProgram(HANDLE);
      DwGLSLProgram.getProgramValidateStatus(gl, HANDLE);
      DwGLSLProgram.getProgramInfoLog(gl, HANDLE, ">> PROGRAM_INFOLOG: "+name+":\n");
  
      DwGLError.debug(gl, "DwGLSLProgram.build");
      
      uniform_loc.clear();
    }
    return this;
  }


  // Query information
  public static void getProgramInfoLog(GL2ES2 gl, int program_id, String info) {
    if(program_id==-1) return;
    
    IntBuffer log_len = IntBuffer.allocate(1);
    gl.glGetProgramiv(program_id, GL2ES2.GL_INFO_LOG_LENGTH, log_len);

    ByteBuffer buffer = ByteBuffer.allocate(log_len.get(0));
    gl.glGetProgramInfoLog(program_id, log_len.get(0), null, buffer);
    
//    buffer.put(log_len.get(0)-1, (byte) ' ');
    String log = Charset.forName("US-ASCII").decode(buffer).toString();
    
    if( log.length() > 1 && log.charAt(0) != 0){
      System.out.println(info);
      System.out.println(log);
    }
  }
  

  public static void getProgramValidateStatus(GL2ES2 gl, int program_id) {
    if(program_id==-1) return;
    
    IntBuffer log_len = IntBuffer.allocate(1);
    gl.glGetProgramiv(program_id, GL2ES2.GL_VALIDATE_STATUS, log_len);
    
    ByteBuffer buffer = ByteBuffer.allocate(log_len.get(0));
    gl.glGetProgramInfoLog(program_id, log_len.get(0), null, buffer);
    
    String log = Charset.forName("US-ASCII").decode(buffer).toString();
    
    if( log.length() > 1 && log.charAt(0) != 0){
      System.out.println(log);
    }
  }
  
  
  
  
  


  
  // Comfort Methods
  public void begin(){
    build();
    gl.glUseProgram(HANDLE);
  }
  
  public void end(){
    clearUniformTextures();
    gl.glUseProgram(0);
  }
  
  
  
  HashMap<String, Integer> uniform_loc = new HashMap<String, Integer>();

  public boolean LOG_WARNINGS = true;
  
  int warning_count = 0;
  
  private int getUniformLocation(String uniform_name){
    int LOC_name = -1;
    Integer loc = uniform_loc.get(uniform_name);
    if(loc != null){
      LOC_name = loc;
    } else {
      LOC_name = gl.glGetUniformLocation(HANDLE, uniform_name);
      if(LOC_name != -1){
        uniform_loc.put(uniform_name, LOC_name);
      }
    }
    if(LOC_name == -1){
      if(LOG_WARNINGS && warning_count < 20){
        System.out.println(name+": uniform location \""+uniform_name+"\" = -1");
        warning_count++;
      }
    }
    return LOC_name;
  }
  
 
  
  
  public static class UniformTexture{
    String name = null;;
    int loc = -1;
    int loc_idx = -1;
    int target = -1;
    int handle = -1;
    
    public UniformTexture(String name, int loc, int loc_idx, int handle, int target) {
      this.name = name;
      this.loc = loc;
      this.loc_idx = loc_idx;
      this.target = target;
      this.handle = handle;
    }
  }
  
  public Stack<UniformTexture> uniform_textures = new Stack<>();
  
  public int uniformTexture(String uniform_name, DwGLTexture texture){
    return uniformTexture(uniform_name, texture.HANDLE[0], texture.target);
  }
  public int uniformTexture(String uniform_name, DwGLTexture3D texture){
    return uniformTexture(uniform_name, texture.HANDLE[0], texture.target);
  }
  public int uniformTexture(String uniform_name, int HANDLE_tex){
    return uniformTexture(uniform_name,HANDLE_tex, GL2ES2.GL_TEXTURE_2D);
  }
  public int uniformTexture(String uniform_name, int HANDLE_tex, int target){
    int loc = getUniformLocation(uniform_name);
    if(loc != -1){
      UniformTexture untex = new UniformTexture(uniform_name, loc, uniform_textures.size(), HANDLE_tex, target);
      uniform_textures.push(untex);
      
      gl.glUniform1i(loc, untex.loc_idx); 
      gl.glActiveTexture(GL2ES2.GL_TEXTURE0 + untex.loc_idx); 
      gl.glBindTexture(untex.target, untex.handle);
    }
    return uniform_textures.size();
  }
  
  public void clearUniformTextures(){
    while(!uniform_textures.empty()){
      UniformTexture untex = uniform_textures.pop();
      gl.glActiveTexture(GL2ES2.GL_TEXTURE0 + untex.loc_idx); 
      gl.glBindTexture(untex.target, 0);
    }
  }
  
  

  
  
  
  
  public void uniform1fv(String uniform_name, int count, float[] vec1){
    gl.glUniform1fv(getUniformLocation(uniform_name), count, vec1, 0);
  }
  public void uniform2fv(String uniform_name, int count, float[] vec2){
    gl.glUniform2fv(getUniformLocation(uniform_name), count, vec2, 0);
  }
  public void uniform3fv(String uniform_name, int count, float[] vec3){
    gl.glUniform3fv(getUniformLocation(uniform_name), count, vec3, 0);
  }
  public void uniform4fv(String uniform_name, int count, float[] vec4){
    gl.glUniform4fv(getUniformLocation(uniform_name), count, vec4, 0);
  }
  
  
  public void uniformMatrix2fv(String uniform_name, int count, boolean transpose, float[] buffer, int offset){
    gl.glUniformMatrix2fv(getUniformLocation(uniform_name), count, transpose, buffer, offset);
  }
  public void uniformMatrix3fv(String uniform_name, int count, boolean transpose, float[] buffer, int offset){
    gl.glUniformMatrix3fv(getUniformLocation(uniform_name), count, transpose, buffer, offset);
  }
  public void uniformMatrix4fv(String uniform_name, int count, boolean transpose, float[] buffer, int offset){
    gl.glUniformMatrix4fv(getUniformLocation(uniform_name), count, transpose, buffer, offset);
  }
  
  
  public void uniformMatrix2fv(String uniform_name, int count, float[] buffer, int offset){
    gl.glUniformMatrix2fv(getUniformLocation(uniform_name), count, false, buffer, offset);
  }
  public void uniformMatrix3fv(String uniform_name, int count, float[] buffer, int offset){
    gl.glUniformMatrix3fv(getUniformLocation(uniform_name), count, false, buffer, offset);
  }
  public void uniformMatrix4fv(String uniform_name, int count, float[] buffer, int offset){
    gl.glUniformMatrix4fv(getUniformLocation(uniform_name), count, false, buffer, offset);
  }
  
  
  
  public void uniform1f(String uniform_name, float v0){
    gl.glUniform1f(getUniformLocation(uniform_name), v0);
  }
  public void uniform2f(String uniform_name, float v0, float v1){
    gl.glUniform2f(getUniformLocation(uniform_name), v0, v1);
  }
  public void uniform3f(String uniform_name, float v0, float v1, float v2){
    gl.glUniform3f(getUniformLocation(uniform_name), v0, v1, v2);
  }
  public void uniform4f(String uniform_name, float v0, float v1, float v2, float v3){
    gl.glUniform4f(getUniformLocation(uniform_name), v0, v1, v2, v3);
  }
  
  
  public void uniform1i(String uniform_name, int v0){
    gl.glUniform1i(getUniformLocation(uniform_name), v0);
  }
  public void uniform2i(String uniform_name, int v0, int v1){
    gl.glUniform2i(getUniformLocation(uniform_name), v0, v1);
  }
  public void uniform3i(String uniform_name, int v0, int v1, int v2){
    gl.glUniform3i(getUniformLocation(uniform_name), v0, v1, v2);
  }
  public void uniform4i(String uniform_name, int v0, int v1, int v2, int v3){
    gl.glUniform4i(getUniformLocation(uniform_name), v0, v1, v2, v3);
  }
  
  

  
  public void drawFullScreenQuad(int[] viewport){
    if(viewport != null){
      drawFullScreenQuad(viewport[0], viewport[1], viewport[2], viewport[3]);
    } else {
      drawFullScreenQuad();
    }
  }
  
  
  public void drawFullScreenQuad(int x, int y, int w, int h){
    gl.glViewport(x, y, w, h);
    drawFullScreenQuad();
  }
  public void drawFullScreenQuad(){
    gl.glDrawArrays(GL2ES2.GL_TRIANGLE_STRIP, 0, 4);
  }
  
  
  public void drawFullScreenLines(int x, int y, int w, int h, int num_lines, float line_width){
    drawFullScreenLines(x,y,w,h,num_lines,line_width, true);
  }
  
  public void drawFullScreenLines(int x, int y, int w, int h, int num_lines, float line_width, boolean smooth){
    gl.glViewport(x, y, w, h);
    
    if(smooth){
      gl.glEnable(GL2.GL_LINE_SMOOTH);
      gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_FASTEST);
//      gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
    } else {
      gl.glDisable(GL2.GL_LINE_SMOOTH);
    }
    gl.glLineWidth(line_width);
    
    gl.glDrawArrays(GL2.GL_LINES, 0, num_lines * 2);
  }
  
  
  public void drawFullScreenPoints(int x, int y, int w, int h, int num_points){
    drawFullScreenPoints(x,y,w,h,num_points,true);
  }
  
  public void drawFullScreenPoints(int x, int y, int w, int h, int num_points, boolean smooth){
    gl.glViewport(x, y, w, h);

    gl.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
    gl.glEnable(GL2.GL_POINT_SPRITE);
    gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);
    
    if(smooth){
      gl.glEnable(GL2.GL_POINT_SMOOTH);
      gl.glHint(GL2.GL_POINT_SMOOTH_HINT, GL2.GL_FASTEST);
    } else {
      gl.glDisable(GL2.GL_POINT_SMOOTH);
    }
    DwGLError.debug(gl, "DwGLSLProgram.drawFullScreenPoints");
    gl.glDrawArrays(GL2.GL_POINTS, 0, num_points);
  }
  
  
  
  public void drawFullScreenQuads(int x, int y, int w, int h, int num_quads){
    gl.glViewport(x, y, w, h);
//    gl.glDrawArrays(GL2.GL_QUADS, 0, num_quads * 1);
    GL2ES3 gl2es3 = gl.getGL2ES3();
    gl2es3.glDrawArraysInstanced(GL2.GL_TRIANGLE_STRIP, 0, 4, num_quads);  //draw #count quads
  }
  
  
  
  

  
}