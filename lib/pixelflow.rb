if RUBY_PLATFORM == 'java'
  require 'PixelFlow.jar'

  def import_class_list(list, string)
    list.each { |klass| java_import format(string, klass) }
  end

  module PixelFlow
    java_import 'com.thomasdiewald.pixelflow.java.DwPixelFlow'
  end

  module ShaderToy
    java_import 'com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy'
  end

  module Fluid
    java_import 'com.thomasdiewald.pixelflow.java.fluid.DwFluid2D'
  end
end
