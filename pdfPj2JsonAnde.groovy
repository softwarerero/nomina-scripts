#!/usr/bin/env groovy
/***
 Parse PDF with employees of Petropar and generate JSON.
 THIS DOES NOT WORK + PDFBox does not parse right here.
 ***/
@Grab(group='org.apache.pdfbox', module='pdfbox', version='1.8.2')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.util.PDFTextStripper

def outFile = new File('../nomina.sun.com.py/vendor/scripts/nomina.data.js')

def lines = linesFromPdf(new File("../data/ande.pdf"))
lines = filterLines(lines)
//println "lines: " + lines
def json = lines.collect { line2Json(it) }
//println("json: $json")
println "lines: " + lines.size()
addJson2File(json.toString(), outFile)


static def linesFromPdf(pdf) {
	def WORD_SEPARATOR = ";"
	def document = PDDocument.load(pdf)
	def stripper = new PDFTextStripper()
	stripper.setWordSeparator(WORD_SEPARATOR)
	def st = stripper.getText(document)
	def lines = st.split( '\n' )
	document.close()
	lines
}

static def filterLines(lines) {
	lines = lines.findAll { it.split(";").size() == 4 }
	//		lines.each { println it.split(";").size() }
	lines
}


static def line2Json(line) {
	def fields = line.split(";")
	def builder = new groovy.json.JsonBuilder()
	def map = [:]
	map["nombre"] = fields[0].replaceAll("\\d","")
	map["cargo"] = fields[1]
	map["sueldo"] = fields[2].replaceAll("\\.", "").toInteger()
	map["total"] = fields[2].replaceAll("\\.", "").toInteger()
	map["estado"] = fields[3].replaceAll("\\d","")
	map["antiguedad"] = (2013 - fields[3].replaceAll("\\D","").replaceAll("\\.", "").toInteger()).toString()
	map["institucion"] = "Ande"
	builder(map)
	builder.toString()
}

static def addJson2File(json, file) {
	def fileText = file.exists() ? file.text : ""
	fileText = fileText.replace("]", ",")
	json = json.replace("[", "")
	def text = fileText + json
	file.write(text)
}
