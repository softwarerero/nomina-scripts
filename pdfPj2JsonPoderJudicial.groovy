#!/usr/bin/env groovy
/***
	Parse PDF with court employees and insert them into EL.
***/
@Grab(group='org.apache.pdfbox', module='pdfbox', version='1.8.2')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.util.PDFTextStripper


def lines = linesFromPdf(new File("../data/nomina-de-magistrados-y-funcionarios.pdf"))
def json = lines.collect { line2Json(it) }
json = json.findAll { it != null }
def f = new File('../nomina.sun.com.py/vendor/scripts/nomina.data.js')
f.write("window.nominaData = " + json.toString())


static def linesFromPdf(pdf) {
	def WORD_SEPARATOR = ";"
	def document = PDDocument.load(pdf)
	def stripper = new PDFTextStripper()
	stripper.setWordSeparator(WORD_SEPARATOR)
	def st = stripper.getText(document)
	def lines = st.split( '\n' )
	lines = filterLines(lines)
	println "lines: " + lines.size()
	document.close()
	lines
}

static def filterLines(lines) {
	def beforeReferences = true
	def newLines = []
	lines.each { line ->
		if(line.contains("REFERENCIAS:")) beforeReferences = false
		if(beforeReferences) {
			if(!line.contains("NOMBRE Y APELLIDO")) {
				if(!line.contains("Página")) {
					newLines += line
				}
			}
		}
	}
	newLines
}

static def line2Json(line) {
	def builder = new groovy.json.JsonBuilder()
	def fields = line.split(";")
	def map = [:]
	if(fields.size() > 7) {
		map["nombre"] = fields[0]
		map["cargo"] = fields[1].trim()
		map["categoria"] = fields[2]
		map["sueldo"] = fields[3].replaceAll("\\.", "").toInteger()
		map["extra"] = fields[4].replaceAll("\\.", "").toInteger() + fields[5].replaceAll("\\.", "").toInteger()
		map["total"] = fields[3].replaceAll("\\.", "").toInteger() + fields[4].replaceAll("\\.", "").toInteger() + fields[5].replaceAll("\\.", "").toInteger()
		map["antiguedad"] = fields[6]
		map["estado"] = fields[7]
		map["institucion"] = "Poder Judicial"
  	builder(map)
	  builder.toString()
	} else {
		null
	}
}
