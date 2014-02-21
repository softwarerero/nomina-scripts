#!/usr/bin/env groovy
/***
 Parse PDF with employees of Dinac and generate JSON.
 THIS DOES NOT WORK + PDFBox does not parse right here.
 ***/
//@Grab(group='org.apache.pdfbox', module='pdfbox', version='1.8.2')
//import org.apache.pdfbox.pdmodel.PDDocument
//import org.apache.pdfbox.util.PDFTextStripper
import org.jpedal.PdfDecoder


def outFile = new File('test.data.js')

def lines = linesFromPdf("../data/LISTADO_FUNCIONARIOS_DINAC.pdf")
//lines = filterLines(lines)
//def four = lines.findAll { it.split(";").size() ==4 }
//println "four: ${four.size()}"
//def json = lines.collect { line2Json(it) }
//addJson2File(json.toString(), outFile)


static def linesFromPdf(file_name) {
	def WORD_SEPARATOR = ";"
	//	File file = new File()
	def decodePdf = new PdfDecoder(false)
	decodePdf.setExtractionMode(PdfDecoder.TEXT); //extract just text
	PdfDecoder.init(true)
	decodePdf.useTextExtraction()
	decodePdf.openPdfFile(file_name)
	//	def lines = decodePdf.getIO().get
	1.upto(decodePdf.getPageCount() + 1) { page ->
		decodePdf.decodePage(page)
		println page

		/** create a grouping object to apply grouping to data*/
		def currentGrouping = decodePdf.getGroupingObject()

		/**use whole page size for  demo - get data from PageData object*/
		def currentPageData = decodePdf.getPdfPageData()

		int x1 = currentPageData.getMediaBoxX(page)
		int x2 = currentPageData.getMediaBoxWidth(page)+x1

		int y2 = currentPageData.getMediaBoxX(page)
		int y1 = currentPageData.getMediaBoxHeight(page)-y2

		def words =null;

		currentGrouping.e
		words = currentGrouping.extractTextAsWordlist(
				x1,
				y1,
				x2,
				y2,
				page,
				true, "&:=()!;.,\\/\"\"\'\'")
		
		words = words.
		println "words: " + words
		
		
	}

//	println "lines: " + lines
	/*
	 def document = PDDocument.load(pdf)
	 def stripper = new PDFTextStripper()
	 stripper.setWordSeparator(WORD_SEPARATOR)
	 def st = stripper.getText(document)
	 st = st.replace(",00", "")
	 //.replaceAll(" -", " 0.0")
	 def lines = st.split( '\n' )
	 //	lines = filterLines(lines)
	 //	lines.each { println it.size() }
	 //	println "lines: " + lines
	 //	println "lines: " + lines.size()
	 document.close()
	 */
//	lines
}

static def filterLines(lines) {
	lines = lines.findAll { it.split(";").size() > 3 }
	lines
}

static def line2Json(line) {
	def builder = new groovy.json.JsonBuilder()
	def fields = line2Fields(line)
	//	def fields = line.split(";")
	//	println "fields.size: " + fields.size()
	//	println fields[3]
	/*
	 try {
	 def map = [:]
	 map["nombre"] = fields[0]
	 map["cargo"] = fields[1]
	 map["categoria"] = fields[2]
	 map["sueldo"] = fields[3].replaceAll("\\.", "").toInteger()
	 map["extra"] = fields[4].replaceAll("\\.", "").toInteger()
	 map["total"] = fields[3].replaceAll("\\.", "").toInteger() + fields[4].replaceAll("\\.", "").toInteger()
	 map["tipo"] = fields[5]
	 def antiguedad = calcAntiguedad(fields[6])
	 map["antiguedad"] = antiguedad
	 map["institucion"] = "Dinac"
	 builder(map)
	 builder.toString()	
	 } catch(Exception e) {
	 e.printStackTrace()
	 println "line: $line"
	 println "fields.size: " + fields.size()
	 }
	 */
	""
}

static def line2Fields(line) {
	def fields = line.split(";")
	def size = fields.size()
	//	println "size: $size"
	if(size == 4) {
		//		def fields2 = []
		//		fields2 += fields[0]
		//	 println fields
	}
	/*	
	 if(size == 6) {
	 def f1 = fields[0]
	 def ws = f1.split(" ")
	 def wsSize = ws.size()
	 def fields2 = []
	 def name = ws.collect { it == ws[wsSize-1] ? "" : it + " " }
	 fields2 += name.join().trim()
	 fields2 += ws[wsSize-1]
	 fields2.addAll(fields.drop(1))
	 return fields2
	 } else {
	 return fields
	 }
	 */
}

static def calcAntiguedad(field) {
	def pos = field.indexOf("Años")
	def year = (new Date()).getYear()
	println("Year: $year")
	def ret = ""
	if(pos > -1) {
		""
	} else {
		ret = year
	}
	ret.toString()
}

static def addJson2File(json, file) {
	def fileText = file.exists() ? file.text : ""
	fileText = fileText.replace("]", ",")
	json = json.replace("[", "")
	def text = fileText + json
	file.write(text)
}
