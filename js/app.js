angular.module("pouchapp", ["ui.router"])

  .run(function ($pouchDB) {
    $pouchDB.setDatabase("guesswhere");
    $pouchDB.sync("http://188.226.151.146:4984/guesswhere/");
  })

  .config(function ($stateProvider, $urlRouterProvider) {
    $stateProvider
      .state("login", {
        "url": "/",
        "templateUrl": "templates/login.html",
        "controller": "LoginController"
      })
      .state("home", {
        "url": "/home/:nickname/:questionId",
        "templateUrl": "templates/home.html",
        "controller": "HomeController"
      });
    $urlRouterProvider.otherwise("/");
  })

  .controller("LoginController", function ($scope, $rootScope, $state, $stateParams, $pouchDB) {

    // Save a document with either an update or insert
    $scope.save = function (nickname) {
      $pouchDB.get('_local/nickname')
        .then(function (res) {
          console.log(res);
          if (res) {
            $pouchDB.delete(res._id, res._rev)
              .then(function (res) {
                console.log(res);
                $pouchDB.save({
                  _id: '_local/nickname',
                  nickname: nickname
                }).then(function () {
                  $state.go("home", {nickname: nickname, questionId: 'question-1'});
                }, function (error) {
                  console.log(error);
                });
              });
          }
        }, function(err) {
          $pouchDB.save({
            _id: '_local/nickname',
            nickname: nickname
          }).then(function () {
            $state.go("home", {nickname: nickname, questionId: 'question-1'});
          }, function (error) {
            console.log(error);
          });
        });
    }
  })

  .controller("HomeController", function ($scope, $rootScope, $state, $stateParams, $pouchDB, $timeout) {

    $scope.hideStatus = true;

    $pouchDB.startListening();

    // Listen for changes which include create or update events
    $rootScope.$on("$pouchDB:change", function (event, data) {
      if (data.doc.type == "question") {
        $pouchDB.get('_local/nickname')
          .then(function (res) {
            console.log(res);
            if ($stateParams.nickname == res.nickname) {
              $state.go("home", {nickname: res.nickname, questionId: data.doc._id});
            } else {
              $state.go("login", {});
            }
          }, function (err) {
            $state.go("login", {});
          });

      }
    });

    // Listen for changes which include only delete events
    $rootScope.$on("$pouchDB:delete", function (event, data) {
    });

    $scope.save = function (answer) {
      var properties = {
        type: "answer",
        nickname: $stateParams.nickname,
        question_id: $stateParams.questionId,
        user_answer: answer,
        time: Date.now()
      };
      $pouchDB.save(properties)
        .then(function (res) {
          console.log(res);
          $scope.hideStatus = false;
          $timeout(function () {
            $scope.hideStatus = true;
          }, 1000);
          $state.go("home", {nickname: $stateParams.nickname, questionId: "question-" + (parseInt($stateParams.questionId.split("-")[1]) + 1)});
        }, function (err) {
          console.log(err);
        });
    };

    // Look up a document if we landed in the info screen for editing a document
    if ($stateParams.questionId) {
      $pouchDB.get($stateParams.questionId).then(function (result) {
        console.log(result);
        $scope.question = result;
        $scope.$apply();
      });
    }

    $scope.delete = function (id, rev) {
      $pouchDB.delete(id, rev);
    }

  })

  .service("$pouchDB", ["$rootScope", "$q", function ($rootScope, $q) {

    var database;
    var changeListener;

    this.setDatabase = function (databaseName) {
      database = new PouchDB(databaseName);
    };

    this.startListening = function () {
      changeListener = database.changes({
        live: true,
        include_docs: true,
        since: 'now'
      }).on("change", function (change) {
        if (!change.deleted) {
          $rootScope.$broadcast("$pouchDB:change", change);
        } else {
          $rootScope.$broadcast("$pouchDB:delete", change);
        }
      });
    }

    this.stopListening = function () {
      changeListener.cancel();
    }

    this.sync = function (remoteDatabase) {
      database.sync(remoteDatabase, {live: true, retry: true});
    }

    this.save = function (jsonDocument) {
      var deferred = $q.defer();
      if (!jsonDocument._id) {
        database.post(jsonDocument).then(function (response) {
          deferred.resolve(response);
        }).catch(function (error) {
          deferred.reject(error);
        });
      } else {
        database.put(jsonDocument).then(function (response) {
          deferred.resolve(response);
        }).catch(function (error) {
          deferred.reject(error);
        });
      }
      return deferred.promise;
    }

    this.delete = function (documentId, documentRevision) {
      return database.remove(documentId, documentRevision);
    }

    this.get = function (documentId) {
      return database.get(documentId);
    }

    this.destroy = function () {
      database.destroy();
    }

  }]);
