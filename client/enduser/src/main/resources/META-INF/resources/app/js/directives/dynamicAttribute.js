/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
'use strict';

angular.module('self')
        .directive('dynamicAttribute', function ($filter) {
          return {
            restrict: 'E',
            templateUrl: 'views/dynamicAttribute.html',
            scope: {
              schema: "=",
              index: "=",
              user: "="
            },
            controller: function ($scope, $element, $window) {
              $scope.initAttribute = function (schema, index) {

                switch (schema.type) {
                  case "Long":
                  case "Double":
                    $scope.user.plainAttrs[schema.key].values[index] = Number($scope.user.plainAttrs[schema.key].values[index])
                            || undefined;
                    break;
                  case "Enum":
                    $scope.enumerationValues = [];
                    var enumerationValuesSplitted = schema.enumerationValues.toString().split(";");
                    for (var i = 0; i < enumerationValuesSplitted.length; i++) {
                      $scope.enumerationValues.push(enumerationValuesSplitted[i]);
                    }
                    $scope.user.plainAttrs[schema.key].values[index] = $scope.user.plainAttrs[schema.key].values[index]
                            || $scope.enumerationValues[0];
                    break;
                  case "Binary":

                    $scope.userFile = $scope.userFile || '';
                    //for multivalue fields 
//                    $scope.fileInputId = "fileInputId_" + index;

                    $element.bind("change", function (changeEvent) {
                      $scope.$apply(function () {
                        var reader = new FileReader();
                        var file = changeEvent.target.files[0];
                        $scope.userFile = file.name;
                        reader.onload = function (readerEvt) {
                          var binaryString = readerEvt.target.result;
                          $scope.user.plainAttrs[schema.key].values[index] = btoa(binaryString);
                        };
                        reader.readAsBinaryString(file);
                      });
                    });

                    $scope.download = function () {
                      var byteString = atob($scope.user.plainAttrs[schema.key].values[index]);

                      var ab = new ArrayBuffer(byteString.length);
                      var ia = new Uint8Array(ab);
                      for (var i = 0; i < byteString.length; i++) {
                        ia[i] = byteString.charCodeAt(i);
                      }

                      var blob = new Blob([ia], {type: schema.mimeType});

                      saveAs(blob, schema.key);
                    };
                    $scope.remove = function () {
                      $scope.user.plainAttrs[schema.key].values.splice(index, 1);
                      $scope.userFile = '';
                      $("#fileInput").replaceWith($("#fileInput").clone(true));
                    };
                    break;
                  case "Date":

                    $scope.selectedDate = $scope.user.plainAttrs[schema.key].values[index];
                    $scope.format = $scope.schema.conversionPattern;
                    $scope.includeTimezone = false;
                    if ($scope.schema.conversionPattern.indexOf(".SSS") > -1) {
                      $scope.format = $scope.format.replace(".SSS", ".sss");
                    }
                    if ($scope.schema.conversionPattern.indexOf("Z") > -1) {
                      $scope.includeTimezone = true;
                      $scope.format = $scope.format.replace("Z", "");
                    }
                    if ($scope.schema.conversionPattern.indexOf("\'") > -1) {
                      $scope.format = $scope.format.replace(new RegExp("\'", "g"), "");
                    }

                    $scope.bindDateToModel = function (selectedDate, format) {
                      var newFormat = $scope.includeTimezone ? format.concat(" Z") : format;
                      if (selectedDate) {
                        selectedDate = $filter('date')(selectedDate, newFormat);
                        var dateGood = selectedDate.toString();
                        $scope.user.plainAttrs[schema.key].values[index] = dateGood;
                      } else {
                        $scope.user.plainAttrs[schema.key].values[index] = selectedDate;
                      }
                    };

                    $scope.clear = function () {
                      $scope.user.plainAttrs[schema.key].values[index] = null;
                    };

                    // Disable weekend selection
                    $scope.disabled = function (date, mode) {
                      // example if you want to disable weekends
                      // return (mode === 'day' && (date.getDay() === 0 || date.getDay() === 6));
                      return false;
                    };

                    $scope.toggleMin = function () {
                      $scope.minDate = $scope.minDate ? null : new Date();
                    };

                    $scope.maxDate = new Date(2050, 5, 22);

                    $scope.open = function ($event) {
                      $scope.status.opened = true;
                    };

                    $scope.setDate = function (year, month, day) {
                      $scope.user.plainAttrs[schema.key].values[index] = new Date(year, month, day);
                    };

                    $scope.dateOptions = {
                      startingDay: 1
                    };

                    $scope.status = {
                      opened: false
                    };

                    var tomorrow = new Date();
                    tomorrow.setDate(tomorrow.getDate() + 1);
                    var afterTomorrow = new Date();
                    afterTomorrow.setDate(tomorrow.getDate() + 2);
                    $scope.events =
                            [
                              {
                                date: tomorrow,
                                status: 'full'
                              },
                              {
                                date: afterTomorrow,
                                status: 'partially'
                              }
                            ];

                    $scope.getDayClass = function (date, mode) {
                      if (mode === 'day') {
                        var dayToCheck = new Date(date).setHours(0, 0, 0, 0);

                        for (var i = 0; i < $scope.events.length; i++) {
                          var currentDay = new Date($scope.events[i].date).setHours(0, 0, 0, 0);

                          if (dayToCheck === currentDay) {
                            return $scope.events[i].status;
                          }
                        }
                      }

                    };
                    break;

                  case "Boolean":
                    $scope.user.plainAttrs[schema.key].values[index] =
                            Boolean($scope.user.plainAttrs[schema.key].values[index]) || false;
                    break;

                }
              }
              ;
            },
            replace: true
          };
        });
