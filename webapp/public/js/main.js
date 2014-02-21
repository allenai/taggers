angular.module('taggers', ['ui.bootstrap']);
var TaggersCtrl = function($scope, $http) {
  $scope.taggersModel = { }

  $http.get('/fields').then(function(response) {
    $scope.taggersModel = response.data;
  });

  $scope.showExample = function() {
    $scope.taggersModel.sentences = "The fat black cat was hidden in the dark corner."; 
    $scope.taggersModel.extractors = ""; 
    $scope.taggersModel.taggers = "AnimalTagger := LemmatizedKeywordTagger( cat )";
  }

  $scope.submit = function() {
    $http.post("/", $scope.taggersModel).then(
      function(response) {
        $scope.response = response.data;
        $scope.sentence = response.data.sentences[0];
        $scope.level = $scope.sentence.levels[0];
        $scope.responseString = angular.toJson(response.data, pretty=true);
      }
    )
  }
}
