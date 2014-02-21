#!/usr/bin/env groovy
/***
	Parse PDF with employees of Banco Central and insert them into EL.
***/
@Grab(group='org.apache.pdfbox', module='pdfbox', version='1.8.2')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.util.PDFTextStripper

def outFile = new File('../nomina.sun.com.py/vendor/scripts/nomina.data.js')

def lines = linesFromPdf(new File("../data/bancoCentralTabla_Autoridades.pdf"))
def json = lines.collect { line2Json(it) }
addJson2File(json.toString(), outFile)

def lines2 = linesFromPdf(new File("../data/bancoCentralSegunda_parte_Jefaturas.pdf"))
def json2 = lines2.collect { line2JsonV2(it) }
addJson2File(json2.toString(), outFile)

def lines3 = linesFromPdf(new File("../data/bancoCentralTercera_Parte.pdf"))
def json3 = lines3.collect { line2JsonV3(it) }
addJson2File(json3.toString(), outFile)


static def linesFromPdf(pdf) {
	def WORD_SEPARATOR = ";"
	def document = PDDocument.load(pdf)
	def stripper = new PDFTextStripper()
	stripper.setWordSeparator(WORD_SEPARATOR)
	def st = stripper.getText(document)
	def lines = st.split( '\n' )
	lines = filterLines(lines)
	//lines.each { println it }
	//println "lines: " + lines
	println "lines: " + lines.size()
	document.close()
	lines
}

static def filterLines(lines) {
//	def beforeReferences = true
	def newLines = []
	def stopTokens = ["Banco Central del Paraguay", "Listado de Funcionarios", "Segunda Parte", "Jefaturas de", "Apellidos", "Autoridades", "Administrativos", "Tercera Parte", "Primera Parte", "Gestión Admininistrativa", "Responsabilidad", "Jerárquica", "Representación", "Gastos de", "Contratados"]
	lines.each { line ->
 	 def tokenFound = false
		stopTokens.each { token ->
		  if(line.indexOf(token) > -1) {
		  	tokenFound = true
		  }
		}
		if(!tokenFound) {
			newLines += line	  
		}
	}
	newLines
}

static def line2Json(line) {
	def builder = new groovy.json.JsonBuilder()
	def fields = line.split(";")
	def map = [:]
	map["nombre"] = fields[0] + " " + fields[1]
	map["cargo"] = fields[2]
	map["sueldo"] = fields[3].replaceAll("\\.", "").toInteger()
	map["extra"] = fields[4].replaceAll("\\.", "").toInteger() + fields[5].replaceAll("\\.", "").toInteger() + fields[6].replaceAll("\\.", "").toInteger()
	map["total"] = fields[7].replaceAll("\\.", "").toInteger()
	map["institucion"] = "Banco Central"
	builder(map)
	builder.toString()	
}

static def line2JsonV2(line) {
	def builder = new groovy.json.JsonBuilder()
	def fields = line.split(";")
	def map = [:]
	map["nombre"] = fields[0] + " " + fields[1]
	map["cargo"] = fields[2]
	map["sueldo"] = fields[3].replaceAll("\\.", "").toInteger()
	map["extra"] = fields[4].replaceAll("\\.", "").toInteger() 
	map["total"] = fields[5].replaceAll("\\.", "").toInteger()
	map["institucion"] = "Banco Central"
	builder(map)
	builder.toString()
}

static def line2JsonV3(line) {
try{
	def builder = new groovy.json.JsonBuilder()
	def fields = line.split(";")
	def map = [:]
	map["nombre"] = fields[0] + " " + fields[1]
	map["cargo"] = "Administrativo"
	map["sueldo"] = fields[2].replaceAll("\\.", "").toInteger()
	map["total"] = fields[2].replaceAll("\\.", "").toInteger()
	map["institucion"] = "Banco Central"
	builder(map)
	builder.toString()
	} catch(Exception e) {
	e.printStackTrace()
	println line
	}
}

static def addJson2File(json, file) {
	def fileText = file.exists() ? file.text : ""
  fileText = fileText.replace("]", ",")
  json = json.replace("[", "")
  def text = fileText + json
  file.write(text)
}
