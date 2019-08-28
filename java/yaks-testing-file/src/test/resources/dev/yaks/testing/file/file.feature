Feature: File steps

  Background:
    Given File
      | filename  | file:///tmp/myfile.txt |

  Scenario: Verify exist 
    Given File : /tmp/myfile.txt
    Then verify exist

  Scenario: Verify can read
    Given File : /tmp/myfile.txt
    Then verify read

  Scenario: Verify can write
    Given File :  /tmp/myfile.txt
    Then verify write

