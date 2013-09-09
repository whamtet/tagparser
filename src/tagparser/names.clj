(ns tagparser.names)

(defonce names (-> (slurp "names.txt") (.split "\n")))
(defonce unique-names
  (set (for [name names]
         (-> name (.split ",") second))))

(def s (format "package com.chunkmapper.reader;

import java.util.Random;

public class NameReader {
	private static Random random = new Random();
	public static String getName() {
		return names[random.nextInt(names.length)];
	}
	private static String[] names = {%s};
}" (apply str (interpose ",\n" unique-names))))

(def f "/Users/matthewmolloy/workspace/chunkmapper2/src/com/chunkmapper/reader/NameReader.java")
(spit f s)

(.exec (Runtime/getRuntime) (str "open " f))
