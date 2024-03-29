/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apiplatform.modules.gkauth.services

import scala.concurrent.Future
import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import play.api.mvc.MessagesRequest

import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.{GatekeeperRoles, LoggedInRequest}

object LdapAuthorisationServiceMockModule {
  val LdapUserName = "Bobby Ldap"
}

trait LdapAuthorisationServiceMockModule {
  self: MockitoSugar with ArgumentMatchersSugar =>

  protected trait BaseLdapAuthorisationServiceMock {
    def aMock: LdapAuthorisationService

    object Auth {

      private def wrap[A](fn: MessagesRequest[A] => Future[Either[MessagesRequest[A], LoggedInRequest[A]]]) = {
        when(aMock.refineLdap[A]).thenReturn(fn)
      }

      def succeeds[A] = {
        wrap[A](msg => successful(Right(new LoggedInRequest(Some(LdapAuthorisationServiceMockModule.LdapUserName), GatekeeperRoles.READ_ONLY, msg))))
      }

      def notAuthorised[A] = {
        wrap[A](msg => successful(Left(msg)))
      }
    }
  }

  object LdapAuthorisationServiceMock extends BaseLdapAuthorisationServiceMock {
    val aMock = mock[LdapAuthorisationService]
  }

}
