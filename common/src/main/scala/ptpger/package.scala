import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

package object ptpger {
  type Login = String Refined MatchesRegex[W.`"(^[A-Za-z]{3,16})$"`.T]
  type Phone = String Refined MatchesRegex[W.`"""[+][\\d]+"""`.T]
}
