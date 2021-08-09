package main

import (
	"log"
	"net/http"
)

func main() {
	server := http.FileServer(http.Dir("./data"))
	http.Handle("/", server)
	if err := http.ListenAndServe(":4000", nil); err != nil {
		log.Fatal(err)
	}
}
