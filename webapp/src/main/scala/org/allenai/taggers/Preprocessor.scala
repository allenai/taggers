package org.allenai.taggers

import java.io.File
import java.io.PrintWriter

import scala.io.Source
import scala.util.control.NonFatal

import org.allenai.common.Resource

object PreprocessorMain extends App {
  case class Config(sentenceInputFile: File = null, outputFile: File = null)

  val parser = new scopt.OptionParser[Config]("taggers-preprocessor") {
    arg[File]("<input>..").action { (file, c) =>
      c.copy(sentenceInputFile = file)
    }.text("sentence file to pre-populate")
    arg[File]("<output>..").action { (file, c) =>
      c.copy(outputFile = file)
    }.text("sentence file to pre-populate")
  }

  parser.parse(args, Config()).foreach { config =>
    run(config)
  }

  def run(config: Config) = {
    val processors = new ThreadLocal[Processor] {
      override def initialValue() = new Processor()
    }
    val BatchSize = 10000

    Resource.using(Source.fromFile(config.sentenceInputFile)) { source =>
      Resource.using(new PrintWriter(config.outputFile)) { writer =>
        var i = 0
        for {
          group <- source.getLines.grouped(BatchSize)
        } {
          println(s"Processing items ${i * BatchSize} through ${(i + 1) * BatchSize}...")
          i = i + 1
          for {
            line <- group.toSeq.par
            processed = processors.get()(line)
          } {
            try {
              val json = Processor.Formats.sentenceJsonFormat.write(processed)
              writer.println(json.compactPrint)
            } catch {
              case NonFatal(e) =>
                println("Exception on line: " + line)
                e.printStackTrace()
            }
          }
        }
      }
    }
  }
}