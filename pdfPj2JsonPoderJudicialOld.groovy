#!/usr/bin/env groovy
/***
	Parse PDF with court employees and insert them into EL.
***/
@Grab(group='org.apache.pdfbox', module='pdfbox', version='1.8.2')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.util.PDFTextStripper


def WORD_SEPARATOR = ";"
def pdf = new File("data/nomina-de-magistrados-y-funcionarios.pdf")
def document = PDDocument.load(pdf)
def stripper = new PDFTextStripper()
stripper.setWordSeparator(WORD_SEPARATOR)
def st = stripper.getText(document)
def lines = st.split( '\n' )
lines = filterLines(lines)
println "lines: " + lines.size()
Lucener.deleteLuceneIndex()
Lucener.importIntoLucene(lines)
println "hits: " + Lucener.luceneCount()

def json = Lucener.exampleSearch()
println "json: " + json
def f = new File('nominaFront/scripts/nomina.data.js')
f.write(json)


def filterLines(lines) {
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


@Grab(group='org.apache.lucene', module='lucene-core', version='4.5.0')
@Grab(group='org.apache.lucene', module='lucene-queryparser', version='4.5.0')
@Grab(group='org.apache.lucene', module='lucene-analyzers-common', version='4.5.0')
import org.apache.lucene.analysis.es.*
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.store.*
import org.apache.lucene.index.*
import org.apache.lucene.analysis.*
import org.apache.lucene.document.*
import org.apache.lucene.search.*
import org.apache.lucene.queryParser.*
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.util.*
class Lucener {

	static def INDEX_PATH = new File("data/lucene")
	static def READER = DirectoryReader.open(FSDirectory.open(INDEX_PATH))
	static def ANALYZER = new SpanishAnalyzer(Version.LUCENE_45)
  static def MAX_DOCS = 100000000
	
	static def importIntoLucene(lines) {
		def config = new IndexWriterConfig(Version.LUCENE_45, ANALYZER)
		def dir = new NIOFSDirectory(INDEX_PATH)
		def writer = new IndexWriter(dir, config)
		writer.commit()

		def errors = 0
		lines.eachWithIndex { line, i ->
			println "line: $line"
			def columns = line.split(";")
			def nombre = columns[0]
			def doc = new Document()
			println "columns: $columns"
			println "columns.count: ${columns.size()}"
//			def startCategoria = columns.size() == 9 ? 3 : 2
			def startCategoria = 2
			if(columns.size() > 7) {
				try {
					addField(doc, "nombre", columns[0])
					addField(doc, "cargo", columns[1])
					addField(doc, "categoria", columns[startCategoria++])
	//			println "startCategoria: $startCategoria"
	//			println "columns[startCategoria]: " + columns[startCategoria].replaceAll("\\.", "")
					def sueldo = columns[startCategoria++].replaceAll("\\.", "").toInteger()
	//			println "sueldo: $sueldo"
					addLongField(doc, "sueldo", sueldo)
					def gastos = columns[startCategoria++].replaceAll("\\.", "").toInteger()
					addLongField(doc, "gastos", gastos)
					def bonificacion = columns[startCategoria++].replaceAll("\\.", "").toInteger()
					addLongField(doc, "bonificacion", bonificacion)
					addLongField(doc, "sum", sueldo + gastos + bonificacion)
					addLongField(doc, "antiguedad", columns[startCategoria++].toInteger())
					addField(doc, "estado", columns[startCategoria++])
					addField(doc, "institucion", "Poder Judicial")
					writer.addDocument(doc)
				} catch(Exception e) {
					// silent
	//				e.printStackTrace()
					errors++
				}
			}
		}
		println("errors: $errors")
		writer.commit()
	}

	static def addField(doc, name, value) {
	//    field = new Field("cargo", columns[1], Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS)
		def f = new Field(name, value, Field.Store.YES, Field.Index.ANALYZED)
		doc.add(f)
	}

	static def addLongField(doc, name, value) {
		def f = new LongField(name, value, Field.Store.YES)
		doc.add(f)
	}

	static def luceneCount() {
		IndexSearcher searcher = new IndexSearcher(READER)
		def booleanQuery = new BooleanQuery()
		booleanQuery.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST)
		TopDocs results = searcher.search(booleanQuery, MAX_DOCS)
		ScoreDoc[] hits = results.scoreDocs
		def numTotalHits = results.totalHits
	}

	static def exampleSearch() {
		def booleanQuery = new BooleanQuery()
		def parser = new QueryParser(Version.LUCENE_45, "nombre", ANALYZER)
		//parser.setDefaultOperator(QueryParser.Operator.AND)
//		def searchText = "\"Maria VERONICA Omar\""
//		def searchText = "\"Maria VERONICA\""
//		booleanQuery.add(parser.parse(searchText.trim()), BooleanClause.Occur.MUST)
		booleanQuery.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST)
		Lucener.luceneQuery(booleanQuery)	
	}
  
	static IndexSearcher searcher = new IndexSearcher(READER)
	
	static def luceneQuery(query) {
		TopDocs results = searcher.search(query, MAX_DOCS)
		ScoreDoc[] hits = results.scoreDocs
		def numTotalHits = results.totalHits
		def json = hits2Json(hits)
		println "found docs: " + numTotalHits
		json
	}

	static def hits2Json(hits) {
		def builder = new groovy.json.JsonBuilder()
		def list = hits.collect { doc2Json(searcher.doc(it.doc)) }
//		println "json: " + list.toString()
//		def map = ["funcionarios": list]
//		builder(map)
//		builder.toString()
//		"{ \"items\": " + list.toString() + "}"
		list.toString()
	}
	
	static def doc2Json(doc) {
		def builder = new groovy.json.JsonBuilder()
		def fields = doc.getFields()
		def map = [:]
		for(def field: fields) {
			map[field.name] = field.stringValue()
		}
//		map["type"] = "nomina"
//		builder(funcionario: map)
		builder(map)
		builder.toString()
//		map
	}

	static def deleteLuceneIndex() {
		def analyzer = new SpanishAnalyzer(Version.LUCENE_45)
		def config = new IndexWriterConfig(Version.LUCENE_45, analyzer)
		def dir = new NIOFSDirectory(INDEX_PATH)
		def writer = new IndexWriter(dir, config)
		writer.deleteAll()
		writer.close()
	}
}