Feature: File steps

  Background:
    Given File
      | filename  | file:///tmp/myfile.txt |

  Scenario: Verify exist 
    Then verify exist

  Scenario: Verify read
    Then verify read

  Scenario: Verify write
    Then verify write

