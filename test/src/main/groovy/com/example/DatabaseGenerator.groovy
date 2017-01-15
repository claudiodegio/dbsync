def wdFile = new File(".")


println "Inizio"
println "WD:${wdFile.absolutePath}"

def builder = new groovy.json.JsonBuilder()

builder.database {
    name  "db2"

   tables ({
        name  "names"

        records((0..3000).collect {
           [
                   cId: new Date().time,
                   name : "Nome $it",
                   dateCreated : new Date(),
                   dateUpdated : new Date()
           ]
          }
        )
    },
   {
       name  "cities"

       records((0..100).collect {
           [
                   cId: new Date().time,
                   name : "City $it",
                   dateCreated : new Date(),
                   dateUpdated : new Date()
           ]
       }
       )
   },
   {
       name  "states"

       records((0..100).collect {
           [
                   cId: new Date().time,
                   state : "state $it",
                   dateCreated : new Date(),
                   dateUpdated : new Date()
           ]
       }
       )
   })
}

def file = new File("db_${new Date().format("yyyyMMdd_HHmmss")}.json")

file.write builder.toPrettyString()

println  "Generated File ${file.absolutePath}"

println "Fine"

