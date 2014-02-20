var TaggersCtrl = function($scope, $http) {
  $http.get('/fields').then(function(response) {
    $scope.taggersModel = response.data;
  });
  $scope.submit = function() {
    $http.post("/", $scope.taggersModel).then(
      function(response) {
        $scope.response = response.data;
        $scope.sentence = response.data.sentences[0];
        $scope.responseString = angular.toJson(response.data, pretty=true);
      }
    )
  }
}
