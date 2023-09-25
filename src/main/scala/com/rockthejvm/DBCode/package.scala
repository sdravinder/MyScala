//package com.demandbase.integration.google
//
//import com.demandbase.account.AccountMatcher
//import com.demandbase.activity.ActivityUpdateTracker
//import com.demandbase.auth.{Tenant, TenantUser}
//import com.demandbase.db.Session
//import com.demandbase.integration.calendar.CalendarContext
//import com.demandbase.integration.exchange.{ExchangeEmailSync, ExchangeServiceIssuer}
//import com.demandbase.kafka.{AbmKafkaProducer, KafkaUtil}
//import com.demandbase.service.Services
//import com.demandbase.util.ArrayExtensions._
//import com.demandbase.util.Utils
//import com.demandbase.model.subscription.Subscription
//import com.demandbase.model.sync.SyncBlacklist
//import com.demandbase.scheduler.impl.SchedulerWorker
//import com.demandbase.sync.CalendarNotification
//import grizzled.slf4j.Logging
//import joptsimple.{BuiltinHelpFormatter, OptionParser}
//import org.joda.time.DateTime
//import org.joda.time.format.ISODateTimeFormat
//import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
//
//import java.util.Arrays
//
///**
// * Scheduled job that synchronizes email history for users who are in the Subscription table in INITIAL status.
// *
// * Created by rajlenin on 7/15/16.
// */
//@DisallowConcurrentExecution
//class GmailSyncTask extends SchedulerWorker with Logging {
//
//  /**
//   * method that does the job processing
//   * @param tenantId
//   * @param ctxt
//   * @param args
//   * @param count
//   * @param usersToProcess (email, startTime, endTime) -> if empty -> then this class will process all the users, else it will process only the user -> email id given.
//   * @return
//   */
//  def processEmails(
//                     tenantId: Long,
//                     ctxt:     JobExecutionContext,
//                     args:     Seq[Any],
//                     count:    Int,
//                     usersToProcess: Seq[(String, String, String)] = Seq()
//                   ): Seq[Any] = {
//    val tenant = Services.authDb.tenant(tenantId)
//    val tracker = new ActivityUpdateTracker(tenantId)(tenant.session)
//    implicit val activityProducer: Option[AbmKafkaProducer] = KafkaUtil.getKafkaActProducer(tenant)
//
//    Utils.timedBeginEnd("Running Email sync task for tenant " + tenant.dbName, Option(this)) {
//      // tenantUserstoCOnsider -> 1. Fetches all the tenant's user -> 2. Then it filters only priority 1 user -> then it removes the Engagio admins from the SEQ.
//      // SEQ[tenantUser] -> [User1[1], User2[2], User3[3], User 4[4]]
//      // userToProcess -> 1, 4
//      // tenantUsers -> 1, 4
//      // userToProcess -> empty
//      // tenantUsers -> 1,2,3,4
//
//
//      val tenantUsers = CalendarContext.tenantUsersToConsider(tenant).
//        filter(f => usersToProcess.isEmpty ||
//          usersToProcess.find(_._1.contains(f.user.email.toLowerCase)).nonEmpty)
//
//      //one BL for whole flow.
//      //Fetching list from sync_blacklist table
//      val blacklist = SyncBlacklist.list()(tenant.session)
//      //if tenant.serviceAccountEnabled -> removes blacklisted user
//      // else only allow user who has googlePermission
//
//      val users = CalendarSyncTask.synchronizableUsers(tenant, tenantUsers, blacklist).sortBy(_.user.id)
//
//      //Order By Id.
//      // [1, 2, 3, 4, 5,  6, 7, 8, 9, 10, 11]
//      // checkpoint -> no checkpoint
//      // if checkpoint found -> lastprocessedId ?? -> 5 = // [6, 7, 8, 9, 10, 11]
//      //converted to map -> key -> emailId, value -> tenantUser
//
//      val usersMap = users.map(sahib => sahib.user.email.toLowerCase -> sahib).toMap
//      // userIdsToProcess is empty if usersToProcess is empty
//      val userIdsToProcess = usersToProcess.foldLeft(Map[Long, (DateTime, DateTime)]()) { (acc, entry) =>
//        if(usersMap.contains(entry._1.toLowerCase)) {
//          val tenantUser = usersMap.get(entry._1.toLowerCase).get
//          val startTime = ISODateTimeFormat.dateTimeParser().parseDateTime(entry._2)
//          val endTime = ISODateTimeFormat.dateTimeParser().parseDateTime(entry._3)
//          acc ++ Map(tenantUser.user.id -> (startTime, endTime))
//        } else acc
//      }
//
//      if (users.nonEmpty) {
//        val isGoogle = CalendarNotification.isGoogle(tenant)
//        val subType = if (isGoogle) Subscription.TYPE_GMAIL else Subscription.TYPE_EXCHANGE
//        val serviceAccountEnabled = Tenant.isServiceAccountEnabled(tenantId)
//        info(s"$subType is syncing with serviceAccountEnabled = $serviceAccountEnabled")
//        val toSyncSubs =
//          Subscription.toSync(
//            users.map(_.user.id).toSet, subType,
//            tenant, usersToProcess = usersToProcess)(Services.authDb.session)
//
//        val toSyncCount = toSyncSubs.size
//        if (toSyncCount > 0) {
//          info(s"Total number of users to sync = $toSyncCount")
//
//          val (completeCount, errorCount) =
//            if (isGoogle)
//              google(tenant, tracker, users, toSyncSubs, serviceAccountEnabled, blacklist, userIdsToProcess)
//            else
//              exchange(tenant, tracker, users, toSyncSubs, serviceAccountEnabled, userIdsToProcess)
//
//          info(
//            s"Total number of users to sync = $toSyncCount, completed = $completeCount, errors = $errorCount"
//          )
//        } else
//          info("No users to sync")
//
//      } else
//        info("No users in this tenant has google auth permissions")
//    }
//    tracker.rebuild()
//    activityProducer.foreach(_.close())
//    args
//  }
//
//  override def run(
//                    tenantId: Long,
//                    ctxt:     JobExecutionContext,
//                    args:     Seq[Any],
//                    count:    Int
//                  ): Seq[Any] = {
//    processEmails(tenantId, ctxt, args, count)
//  }
//
//  private def google(
//                      tenant:                Tenant,
//                      tracker:               ActivityUpdateTracker,
//                      users:                 Seq[TenantUser],
//                      toSyncSubs:            Seq[Subscription],
//                      serviceAccountEnabled: Boolean,
//                      blacklist: Seq[SyncBlacklist],
//                      userIdsToProcess: Map[Long, (DateTime, DateTime)] = Map()
//                    )(implicit activityProducer: Option[AbmKafkaProducer]): (Int, Int) = {
//    val credentials = new GoogleCredentialIssuer(serviceAccountEnabled)
//    process(tenant, users, toSyncSubs) { (user, sub) =>
//      if(userIdsToProcess.nonEmpty) {
//        // specified users to sync
//        new GmailSync(user, sub, tracker, credentials.getCredentials(user)).
//          retrievePastMessages(blacklist, userIdsToProcess)
//      } else {
//        if (sub.status == Subscription.STATUS_INITIAL) {
//          // retrieve all past messages
//          new GmailSync(user, sub, tracker, credentials.getCredentials(user)).retrievePastMessages(blacklist)
//        } else if (tenant.serviceAccountEnabled && sub.status == Subscription.STATUS_ACTIVE) {
//          // if sync is going through service account, then we need to retrieve new messages because we cannot use
//          // push notification
//          new GmailSync(user, sub, tracker, credentials.getCredentials(user)).retrieveNewMessages(blacklist)
//        }
//      }
//    }
//  }
//
//  private def exchange(
//                        tenant:                Tenant,
//                        tracker:               ActivityUpdateTracker,
//                        users:                 Seq[TenantUser],
//                        toSyncSubs:            Seq[Subscription],
//                        serviceAccountEnabled: Boolean,
//                        userIdsToProcess: Map[Long, (DateTime, DateTime)] = Map()
//                      )(implicit activityProducer: Option[AbmKafkaProducer]): (Int, Int) = {
//    val service = new ExchangeServiceIssuer(serviceAccountEnabled)
//    process(tenant, users, toSyncSubs) { (user, sub) =>
//      new ExchangeEmailSync(user, tracker, service.getService(user)).retrievePastMessages(sub)
//    }
//  }
//
//  private def process(
//                       tenant:     Tenant,
//                       users:      Seq[TenantUser],
//                       toSyncSubs: Seq[Subscription]
//                     )(
//                       block: (TenantUser, Subscription) => Unit
//                     ): (Int, Int) = {
//    val toSyncCount = toSyncSubs.size
//    var syncProgressCount, errorCount, completeCount = 0
//    toSyncSubs.foreach { sub =>
//      syncProgressCount += 1
//      val tenantUser = users.filter(_.user.id == sub.userId).head
//      val email = tenantUser.user.email
//      Utils.timed(
//        s"Syncing emails for user($syncProgressCount of $toSyncCount) ${tenantUser.user.id}  domain: ${AccountMatcher
//          .domainFromEmail(email)}",
//        Some(this)
//      ) {
//        try {
//          block(tenantUser, sub)
//          completeCount += 1
//          //Checkpoint -> position position ??
//          // 1 , 2, 3, 4->fail, 5, 6 (CRON job STOPPED)
//          // last processed userId
//          // tenant id
//        } catch {
//          case e: Throwable =>
//            CalendarSyncTask.logMailCalendarError(
//              tenantUser.user.email,
//              "emails",
//              s"($syncProgressCount of $toSyncCount) ${tenantUser.user.id}",
//              e
//            )(tenant.session)
//            errorCount += 1
//        }
//      }
//    }
//    (completeCount, errorCount)
//  }
//
//  def reprocessUserEmails(tenantId: Long,
//                          userEmails: Seq[String], startTime: String, endTime: String): Unit = {
//    val userEmailsToProcess = userEmails.map( userEmail => {
//      (userEmail, startTime, endTime)
//    })
//    processEmails(tenantId, null, Seq(), 0, userEmailsToProcess)
//  }
//}
//
//object GmailSyncTask extends Logging {
//  def main(args: Array[String]): Unit = {
//    val parser = new OptionParser() {
//      acceptsAll(Arrays.asList("t", "tenant")).withRequiredArg().ofType(classOf[Int])
//        .describedAs( "tenant to login and test" )
//      acceptsAll(Arrays.asList("u", "user")).withRequiredArg().ofType(classOf[String])
//        .describedAs( "user email address" )
//
//      acceptsAll(Arrays.asList("c", "command")).withRequiredArg().ofType(classOf[String])
//        .describedAs( "what kind of command to run" ).defaultsTo("runNow")
//
//      acceptsAll(Arrays.asList("s", "startTime")).withRequiredArg().ofType(classOf[String])
//        .describedAs( "start time" )
//      acceptsAll(Arrays.asList("e", "endTime")).withRequiredArg().ofType(classOf[String])
//        .describedAs( "end time" )
//    }
//
//    parser.formatHelpWith(new BuiltinHelpFormatter(100,2))
//    val optSet = parser.parse(args:_*)
//
//    val tenantId = optSet.valueOf("t").toString.toInt
//    val command = optSet.valueOf("c").toString
//
//    val tenant = Services.authDb.tenant(tenantId)
//    implicit val session = tenant.session
//
//    if(command == "reprocessUserEmails") {
//      val userEmails = optSet.valueOf("u").toString.split(",").asSeq
//      val startTime = optSet.valueOf("s").toString
//      val endTime = optSet.valueOf("e").toString
//      new GmailSyncTask().reprocessUserEmails(tenantId, userEmails, startTime, endTime)
//    } else {
//      new GmailSyncTask().run(args.head.toLong, null, Seq(), 0)
//    }
//  }
//}