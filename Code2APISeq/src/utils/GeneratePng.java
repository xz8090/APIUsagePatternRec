package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizJdkEngine;
import guru.nidi.graphviz.engine.GraphvizV8Engine;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;

public class GeneratePng {

	public static void generateImg(String path,String savepath) throws Exception {
		Graphviz.useEngine(new GraphvizV8Engine(), new GraphvizJdkEngine());
		try (InputStream dot = new FileInputStream(new File(path))) {
		    MutableGraph g = new Parser().read(dot);
		    Graphviz.fromGraph(g).render(Format.PNG).toFile(new File(savepath));
		}
		Graphviz.releaseEngine();
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String path = "dots/test.dot";
		String savepath = "pngs/test-1.png";
		try {
			generateImg(path,savepath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
