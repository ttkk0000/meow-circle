package com.ttkk0000.meowcircle.client

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject

suspend fun MobileApi.friends(cursor: Long = 0): MobilePage<FriendBody> = get("friendships",mapOf("cursor" to "$cursor"))
suspend fun MobileApi.friend(action: String,input: FriendInput): MobileRecord<FriendBody> = durablePost("friendships/$action",input,input.clientRequestId)

suspend fun MobileApi.offers(cursor: Long = 0): MobilePage<OfferBody> = get("offers",mapOf("cursor" to "$cursor"))
suspend fun MobileApi.offer(id: Long): MobileRecord<OfferBody> = get("offers/$id")
suspend fun MobileApi.offerVerifications(id: Long): List<VerificationSummary> = get("offers/$id/verifications")
suspend fun MobileApi.petVerifications(id: Long): List<VerificationSummary> = get("pets/$id/verifications")
suspend fun MobileApi.createOffer(input: OfferInput): MobileRecord<OfferBody> = durablePost("offers",input,input.clientRequestId)
suspend fun MobileApi.appointments(cursor: Long = 0): MobilePage<AppointmentBody> = get("appointments",mapOf("cursor" to "$cursor"))
suspend fun MobileApi.appointment(id: Long): MobileRecord<AppointmentBody> = get("appointments/$id")
suspend fun MobileApi.createAppointment(input: AppointmentInput): MobileRecord<AppointmentBody> = durablePost("appointments",input,input.clientRequestId)
suspend fun MobileApi.appointmentAction(id: Long,action: String,input: AppointmentAction): MobileRecord<AppointmentBody> = durablePost("appointments/$id/$action",input,input.clientRequestId)
suspend fun MobileApi.contractScope(id: Long,input: ContractInput): PurposeScope = post("adoptions/$id/contracts/scope",input)
suspend fun MobileApi.contract(id: Long,action: String,input: ContractInput): MobileRecord<AdoptionBody> = sensitivePost("adoptions/$id/contracts/$action",input,input.clientRequestId)
suspend fun MobileApi.returnPlan(id: Long,action: String,input: ReturnInput): MobileRecord<AdoptionBody> = durablePost("adoptions/$id/returns/$action",input,input.clientRequestId)
suspend fun MobileApi.createCase(input: CaseInput): MobileRecord<CaseBody> = durablePost("cases",input,input.clientRequestId)
suspend fun MobileApi.claim(id: Long,input: ClaimInput): MobileRecord<ClaimBody> = durablePost("cases/$id/claims",input,input.clientRequestId)
suspend fun MobileApi.claimAction(id: Long,action: String,input: VersionedAction): MobileRecord<ClaimBody> {
    val command = if (input.clientRequestId.isEmpty()) input.copy(clientRequestId = newRequestKey()) else input
    return durablePost("claims/$id/$action",command,command.clientRequestId)
}
suspend fun MobileApi.lead(id: Long,input: LeadInput): MobileRecord<LeadBody> = post("cases/$id/leads",input)
suspend fun MobileApi.care(id: Long,input: CareInput): MobileRecord<CareBody> = durablePost("adoptions/$id/care_updates",input,input.clientRequestId)
suspend fun MobileApi.createGroup(input: GroupInput): MobileRecord<GroupBody> = durablePost("groups",input,input.clientRequestId)
suspend fun MobileApi.groupAction(id: Long,action: String,input: GroupAction): MobileRecord<GroupBody> = durablePost("groups/$id/$action",input,input.clientRequestId)
suspend fun MobileApi.invite(input: InviteInput): MobileRecord<InvitationBody> = durablePost("invitations",input,input.clientRequestId)
suspend fun MobileApi.invitations(cursor: Long = 0): MobilePage<InvitationBody> = get("invitations",mapOf("cursor" to "$cursor"))
suspend fun MobileApi.invitationAction(id: Long,action: String,input: VersionedAction): MobileRecord<InvitationBody> = post("invitations/$id/$action",input)
suspend fun MobileApi.review(input: ReviewInput): MobileRecord<ReviewBody> {
    val command = if (input.clientRequestId.isEmpty()) input.copy(clientRequestId = newRequestKey()) else input
    return durablePost("reviews",command,command.clientRequestId)
}
suspend fun MobileApi.reviews(cursor: Long = 0): MobilePage<ReviewBody> = get("reviews",mapOf("cursor" to "$cursor"))
suspend fun MobileApi.feedback(input: FeedbackInput): MobileRecord<FeedbackInput> = post("visit_feedback",input)
suspend fun MobileApi.guardians(): List<MobileRecord<GuardianBody>> = get("guardians")
suspend fun MobileApi.guardianScope(id: Long): PurposeScope = post("guardians/$id/scope",buildJsonObject {})
suspend fun MobileApi.guardianDecision(id: Long,input: GuardianDecision): MobileRecord<GuardianBody> = sensitivePost("guardians/$id/decision",input,input.clientRequestId)
suspend fun MobileApi.newDataRequestCapability(): String = post<Map<String,String>,JsonElement>("capability",buildJsonObject {}).getValue("token")
suspend fun MobileApi.dataRequestScope(input: DataRequestInput): PurposeScope = post("data-requests/scope",input)
suspend fun MobileApi.submitDataRequest(input: DataRequestInput): MobileRecord<DataRequestBody> = submitDataRequestSecure(input)
suspend fun MobileApi.findDataRequest(key: String,token: String): DataRequestStatus = get("data-requests/requests/$key",authenticated = false,headers = mapOf("X-Request-Access" to token))
suspend fun MobileApi.dataRequestStatus(id: Long,token: String): DataRequestStatus = get("data-requests/$id",authenticated = false,headers = mapOf("X-Request-Access" to token))
suspend fun MobileApi.newSafetySession(): SafetyAccess = safetySessionSecure()
suspend fun MobileApi.submitSafetyTicket(token: String,input: RightsInput,authenticated: Boolean = false): RightsReceipt = safetySubmitSecure(token,input,authenticated)
suspend fun MobileApi.findSafetyTicket(key: String,token: String): RightsReceipt = get("safety/requests/$key",authenticated = false,headers = mapOf("X-Safety-Token" to token))
suspend fun MobileApi.requestSafetyContactOTP(token: String,email: String): OTPReceipt = post("safety/contact-otp",EmailOTPInput(email,"rights_contact"),false,mapOf("X-Safety-Token" to token))
suspend fun MobileApi.verifySafetyContact(token: String,email: String,code: String): JsonElement = post("safety/contact-verify",EmailAuthInput(email,code),false,mapOf("X-Safety-Token" to token))
suspend fun MobileApi.registerPushDevice(input: PushDeviceInput): PushDeviceReceipt = post("push-devices",input)
suspend fun MobileApi.savePreferences(version: Long,preference: PreferenceBody): MobileRecord<PreferenceBody> = put("preferences",SettingsInput(version,preference = preference))
suspend fun MobileApi.saveExpressions(version: Long,expressions: ExpressionBody): MobileRecord<ExpressionBody> = put("expressions",SettingsInput(version,expressions = expressions))
suspend fun MobileApi.saveSubscription(input: SubscriptionInput): MobileRecord<SubscriptionBody> = put("subscriptions",input)
suspend fun MobileApi.safetyTicketStatus(id: Long,token: String): RightsReceipt = get("safety/tickets/$id",authenticated = false,headers = mapOf("X-Safety-Token" to token))
suspend fun MobileApi.rightsScope(input: RightsInput): PurposeScope = post("safety/scope",input)
