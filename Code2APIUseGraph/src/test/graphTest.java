package test;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.engine.GraphvizJdkEngine;
import guru.nidi.graphviz.engine.GraphvizV8Engine;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;

public class graphTest {

	public void testImg() throws Exception {
		Graphviz.useEngine(new GraphvizV8Engine(), new GraphvizJdkEngine());
		try (InputStream dot = new FileInputStream(new File("dots/color2.dot"))) {
		    MutableGraph g = new Parser().read(dot);
		    Graphviz.fromGraph(g).width(700).render(Format.PNG).toFile(new File("pngs/ex4-1.png"));

		    /*
		    g.graphAttrs()
		            .add(Color.WHITE.gradient(Color.rgb("888888")).background().angle(90))
		            .nodeAttrs().add(Color.WHITE.fill())
		            .nodes().forEach(node ->
		            node.add(
		                    Color.named(node.name().toString()),
		                    Style.lineWidth(4).and(Style.FILLED)));
		    Graphviz.fromGraph(g).width(700).render(Format.PNG).toFile(new File("pngs/ex4-2.png"));
			*/
		}
		Graphviz.releaseEngine();
	}
	public static void main(String[] args) {
		graphTest t = new graphTest();
		try {
			t.testImg();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
