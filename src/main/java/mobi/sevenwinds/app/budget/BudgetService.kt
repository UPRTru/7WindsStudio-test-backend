package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorEntity
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                if (body.author_id != null) {
                    this.author = AuthorEntity(EntityID(body.author_id, AuthorTable))
                }
                this.author = author
            }
            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam):
            BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val query = (BudgetTable leftJoin AuthorTable)
                .select { (BudgetTable.year eq param.year) }
                .orderBy(BudgetTable.month)
                .limit(param.limit, param.offset)
            param.authorName?.let {
                query.andWhere {
                    (AuthorTable.name.isNotNull()) and (AuthorTable.name.lowerCase() like "%${it.toLowerCase()}%")
                }
            }
            query.orderBy(BudgetTable.year to SortOrder.DESC,
                BudgetTable.month to SortOrder.ASC, BudgetTable.amount to SortOrder.DESC)

            val queryYear = BudgetTable.select { BudgetTable.year eq param.year }

            val total = queryYear.count()
            val dataAllInfo = BudgetEntity.wrapRows(query).map { it.toResponseBudgetAuthor() }
            val data = BudgetEntity.wrapRows(queryYear).map { it.toResponse() }
            val types = data.groupBy { it.type.name }.mapValues { it.value.sumOf { v -> v.amount } }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = types,
                items = dataAllInfo
            )
        }
    }
}