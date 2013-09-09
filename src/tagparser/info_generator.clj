(ns tagparser.info-generator)

(def sources ["globcover" "heights" "lakes" "poi" "rivers" "boundaries" "coastlines" "rails"])

(def header "package com.chunkmapper;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class InfoManager {
%s
}
")

(def xapi-map {"rivers" "waterway=river" "poi" "place=*" "rails" "railway=rail%spreserved"
"coastlines" "natural=coastline" "boundaries" "boundary=administrative" "lakes" "natural=water"
})

(defn xapi-url [source]
  (str "http://www.overpass-api.de/api/xapi?way["
       (xapi-map source) "][bbox=%s,%s,%s,%s]"))

(def file-method "public static File %sFile(int regionx, int regionz) {
File parent = new File(Utila.CACHE, \"%s\");
parent.mkdirs();
File f = new File(parent, \"f_\" + regionx + \"_\" + regionz + Utila.BINARY_SUFFIX);
return f;
}\n")

(defn server-method [source] (str "public static URL " source "Server(int regionx, int regionz) throws MalformedURLException {
String s = \"" (xapi-url source) "\";
return new URL(getAddress(s, regionx, regionz));
}\n"))
(def backup-method "public static URL %sBackup(int regionx, int regionz) throws MalformedURLException {
return null;
}\n")

(defn method-group [group]
  (apply str (for [source sources]
                    (format group source source source source))))

(def method-group2
  (apply str (for [source sources]
               (server-method source))))

(def s (format header (str
(method-group file-method) \newline
method-group2 \newline
(method-group backup-method) \newline
"	private static String getAddress(String s, int regionx, int regionz) {
		final double REGION_WIDTH_IN_DEGREES = 512 / 3600.;
		double lon1 = regionx * REGION_WIDTH_IN_DEGREES;
		double lon2 = lon1 + REGION_WIDTH_IN_DEGREES;
		double lat2 = -regionz * REGION_WIDTH_IN_DEGREES;
		double lat1 = lat2 - REGION_WIDTH_IN_DEGREES;

		return String.format(s, lon1, lat1, lon2, lat2);
	}
")))

(defn generate []
(spit "/Users/matthewmolloy/workspace/chunkmapper2/src/com/chunkmapper/InfoManager.java" s)
)

(defn generate-enum []
  (spit "/Users/matthewmolloy/workspace/chunkmapper2/src/com/chunkmapper/enumeration/OSMSource.java"
        (format "package com.chunkmapper.enumeration;

public enum OSMSource {
%s;
}
" (apply str (interpose ", " (drop 2 sources))))))

(defn generate-switch []
  (println
   (format "switch(source) {
%s
}" (apply str (for [source sources]
                (format "case %s:
url = InfoManager.%sServer(regionx, regionz);
break;\n" source source))))))
