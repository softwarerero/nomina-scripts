#!/usr/bin/env groovy
/***
 Parse PDF with employees of Petropar and generate JSON.
 THIS DOES NOT WORK + PDFBox does not parse right here.
 ***/
@Grab(group='org.apache.pdfbox', module='pdfbox', version='1.8.2')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.util.PDFTextStripper

def outFile = new File('../nomina.sun.com.py/vendor/scripts/nomina.data.js')

def lines = linesFromPdf(new File("../data/petropar.pdf"))
lines = filterLines(lines)
//println "lines: " + lines
def json = lines.collect { line2Json(it) }
//println("json: $json")
addJson2File(json.toString(), outFile)


static def linesFromPdf(pdf) {
	def WORD_SEPARATOR = ";"
	def document = PDDocument.load(pdf)
	def stripper = new PDFTextStripper()
	stripper.setWordSeparator(WORD_SEPARATOR)
	def st = stripper.getText(document)
	def lines = st.split( '\n' )
	println "lines: " + lines.size()
	document.close()
	lines
}

static def filterLines(lines) {
	lines = lines.findAll { it.split(";").size() == 6 }
	lines
}


static def line2Json(line) {
	def builder = new groovy.json.JsonBuilder()
	def fields = line2Fields(line)
//	println "fields: ${fields[2]}"
	def map = [:]
	map["nombre"] = fields[2]
	map["cargo"] = fields[3]
	map["sueldo"] = fields[4].replaceAll("\\.", "").toInteger()
	map["extra"] = fields[5].replaceAll("\\.", "").toInteger()
	map["total"] = fields[6].replaceAll("\\.", "").toInteger()
	map["institucion"] = "Petropar"
	builder(map)
	builder.toString()
}

static def line2Fields(line) {
	def fields = line.split(";")
	def size = fields.size()
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
}

static def addJson2File(json, file) {
	def fileText = file.exists() ? file.text : ""
	fileText = fileText.replace("]", ",")
	json = json.replace("[", "")
	def text = fileText + json
	file.write(text)
}
