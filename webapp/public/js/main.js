angular.module('taggers', ['ui.bootstrap']);
var TaggersCtrl = function($scope, $http) {
  $scope.taggersModel = { }

  $http.get('/fields').then(function(response) {
    $scope.taggersModel = response.data;
  });

  $scope.showExample = function() {
    $scope.taggersModel.sentences = "The fat black cat was hidden in the dark corner.\nThe song birds sing new songs in the spring.";
    $scope.taggersModel.extractors = "x: DescribedAnimal => described animal: ${x}";
    $scope.taggersModel.taggers = ">>> Level 1\n\nAnimal := LemmatizedKeywordTagger {\n  cat\n  bird\n  frog\n}" + "\n\n" +
      "DescribedAnimal := TypedOpenRegex ( <pos='JJ'>+ @Animal )";

    $scope.submit()
  }

  $scope.submit = function() {
    $scope.working = true;
    $http.post("/", $scope.taggersModel)
      .success(function(data, status, headers, config) {
        $scope.working = false;

        $scope.errorResponse = undefined
        $scope.response = data;

        $scope.sentence = data.sentences[0];
        $scope.level = $scope.sentence.levels[$scope.sentence.levels.length - 1];
        $scope.highlightedInterval = {
          start: 0,
          end: 0
        }
        $scope.responseString = angular.toJson(data, pretty=true);
      })
      .error(function(data, status, headers, config) {
        $scope.working = false;

        $scope.response = undefined
        $scope.errorResponse = data
        $scope.errorResponse.status = status
      })
  }

  $scope.selectHighlight = function(type) {
    $scope.highlightedInterval = {
      start: type.startIndex,
      end: type.endIndex
    }
  }

  $scope.noHighlight = function() {
    $scope.highlightedInterval = {
      start: 0,
      end: 0
    }
  }
}
