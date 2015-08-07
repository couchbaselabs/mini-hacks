package main

import (
	"log"
	"net/http"
	"encoding/json"
	"io/ioutil"
	"fmt"
)

func main() {

	http.HandleFunc("/invitecode", func(w http.ResponseWriter, r *http.Request) {
		log.Println("ping")

		var data map[string]interface{}

		body, _ := ioutil.ReadAll(r.Body)
		json.Unmarshal(body, &data)
		log.Printf("Send a notification to %s with device tokens %v", data["name"].(string),
			data["registration_ids"].([]interface {}))

		// send notification to all devices

	})

	http.HandleFunc("/new_article", func(w http.ResponseWriter, r *http.Request) {
		log.Println("ping")

		var data map[string]interface{}

		body, _ := ioutil.ReadAll(r.Body)
		json.Unmarshal(body, &data)

		topic := data["topic"].(string)
		log.Printf("Querying user Profiles subscribed to %s", topic)

		var stringUrl string = fmt.Sprintf("http://localhost:4985/db/_design/extras/_view/user_topics?key=\"%s\"", topic)
		res, err := http.Get(stringUrl)

		if err != nil {
			fmt.Print(err)
			return
		}

		if res != nil {

			var result map[string]interface{}

			body, _ = ioutil.ReadAll(res.Body)
			json.Unmarshal(body, &result)

			log.Printf("Result from the user_topics query %v", result["rows"].([]interface {}))
		}

	})

	log.Fatal(http.ListenAndServe(":8000", nil))
}