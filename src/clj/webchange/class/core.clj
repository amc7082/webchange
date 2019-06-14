(ns webchange.class.core
  (:require [webchange.db.core :refer [*db*] :as db]
            [clojure.tools.logging :as log]
            [java-time :as jt]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :refer [->snake_case_keyword]]
            [webchange.auth.core :as auth]))

(defn get-classes [school-id]
  (let [classes (db/get-classes {:school_id school-id})]
    {:classes classes}))

(defn get-class [id]
  (let [class (db/get-class {:id id})]
    class))

(defn with-user
  [{user-id :user-id :as item}]
  (let [user (-> (db/get-user {:id user-id})
                 auth/visible-user)]
    (assoc item :user user)))

(defn get-students-by-class [class-id]
  (let [students (->> (db/get-students-by-class {:class_id class-id})
                      (map #(with-user %))
                      (map #(assoc % :date-of-birth (-> % :date-of-birth str)))
                      (map #(dissoc % :access-code)))]
    {:class-id class-id
     :students students}))

(defn get-student [id]
  (let [student (-> (db/get-student {:id id})
                    (#(assoc % :date-of-birth (-> % :date-of-birth str)))
                    with-user)]
    {:student student}))

(defn create-class!
  [data]
  (let [prepared-data (transform-keys ->snake_case_keyword data)
        [{id :id}] (db/create-class! prepared-data)]
    [true {:id id}]))

(defn update-class!
  [id data]
  (let [prepared-data (assoc data :id id)]
    (db/update-class! prepared-data)
    [true {:id id}]))

(defn delete-class!
  [id]
  (db/delete-class! {:id id})
  [true {:id id}])

(defn create-student!
  [data]
  (let [date-of-birth (jt/local-date "yyyy-MM-dd" (:date-of-birth data))
        transform #(transform-keys ->snake_case_keyword %)
        prepared-data (-> data
                          (assoc :date-of-birth date-of-birth)
                          transform)
        [{id :id}] (db/create-student! prepared-data)]
    [true {:id id}]))

(defn update-student!
  [id data]
  (let [date-of-birth (jt/local-date "yyyy-MM-dd" (:date-of-birth data))
        transform #(transform-keys ->snake_case_keyword %)
        prepared-data (-> data
                          (assoc :date-of-birth date-of-birth)
                          (assoc :id id)
                          transform)]
    (db/update-student! prepared-data)))

(defn update-student-access-code!
  [id data]
  (let [prepared-data (-> (transform-keys ->snake_case_keyword data)
                          (assoc :id id))]
    (db/update-student-access-code! prepared-data)))


(defn delete-student!
  [id]
  (db/delete-student! {:id id})
  [true {:id id}])

(defn get-current-school [] (db/get-first-school))

(defn generate-code [] (str (int (rand 9999))))

(defn code-unique? [school-id code]
  (-> (db/access-code-exists? {:school_id school-id :access_code code}) :result not))

(defn next-code [school-id]
  (loop [i 0
         code (generate-code)]
    (cond
      (code-unique? school-id code) [true {:school-id school-id :access-code code}]
      (> i 100) [false {:errors {:form "Unable to create code"}}]
      :else (recur (inc i) (generate-code)))))
